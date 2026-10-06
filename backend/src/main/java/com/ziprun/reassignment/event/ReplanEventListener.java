package com.ziprun.reassignment.event;

import com.ziprun.reassignment.model.*;
import com.ziprun.reassignment.repository.AgentRepository;
import com.ziprun.reassignment.repository.OrderRepository;
import com.ziprun.reassignment.repository.SuggestionRepository;
import com.ziprun.reassignment.routing.RoutingEngine;
import com.ziprun.reassignment.routing.RoutingRecommendation;
import com.ziprun.reassignment.routing.RoutingStrategy;
import com.ziprun.reassignment.routing.TriggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Asynchronous event listener executing the agentic re-planning loop.
 * Runs on a dedicated thread pool to ensure the PATCH /api/agents/{id}/status thread returns immediately.
 */
@Component
public class ReplanEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReplanEventListener.class);

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final SuggestionRepository suggestionRepository;
    private final RoutingEngine routingEngine;

    public ReplanEventListener(OrderRepository orderRepository,
                               AgentRepository agentRepository,
                               SuggestionRepository suggestionRepository,
                               RoutingEngine routingEngine) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.suggestionRepository = suggestionRepository;
        this.routingEngine = routingEngine;
    }

    @Async
    @EventListener
    @Transactional
    public void handleAgentOfflineEvent(AgentOfflineEvent event) {
        log.info("[Async Re-plan] Processing AgentOfflineEvent for agent {} ({}) on thread [{}]",
                event.agentName(), event.agentId(), Thread.currentThread().getName());

        try {
            // Invalidate existing PENDING suggestions that recommended this now-offline agent
            List<ReassignmentSuggestion> staleSuggestions = suggestionRepository
                    .findByRecommendedAgentIdAndStatus(event.agentId(), SuggestionStatus.PENDING);
            if (!staleSuggestions.isEmpty()) {
                log.info("[Async Re-plan] Invalidating {} stale pending suggestion(s) recommending now-offline agent {}",
                        staleSuggestions.size(), event.agentId());
                staleSuggestions.forEach(s -> s.setStatus(SuggestionStatus.REJECTED));
                suggestionRepository.saveAll(staleSuggestions);
            }

            List<Order> strandedOrders = orderRepository.findByAssignedAgentIdAndStatus(
                    event.agentId(), OrderStatus.ASSIGNED);

            if (strandedOrders.isEmpty()) {
                log.info("[Async Re-plan] No assigned orders stranded for agent {}", event.agentId());
                return;
            }

            int strandedCount = strandedOrders.size();
            log.info("[Async Re-plan] Found {} stranded order(s) for offline agent {}", strandedCount, event.agentId());

            List<Agent> availableAgents = agentRepository.findByStatusIn(List.of(AgentStatus.AVAILABLE, AgentStatus.BUSY));
            TriggerContext context = TriggerContext.agentOffline(event.agentId(), strandedCount);
            RoutingStrategy activeStrategy = routingEngine.getActiveStrategy();

            for (Order order : strandedOrders) {
                // Idempotency Layer 1: Application-level check
                boolean alreadyPending = suggestionRepository.existsByOrderIdAndTriggerReasonAndStatus(
                        order.getId(), TriggerReason.AGENT_OFFLINE, SuggestionStatus.PENDING);

                if (alreadyPending) {
                    log.info("[Async Re-plan] Idempotency: pending re-plan suggestion already exists for order {}", order.getId());
                    continue;
                }

                // Transition order to REASSIGNMENT_PENDING
                order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
                orderRepository.save(order);

                // Compute recommendation
                List<RoutingRecommendation> recommendations = activeStrategy.recommend(order, availableAgents, context);
                if (recommendations == null || recommendations.isEmpty()) {
                    log.warn("[Async Re-plan] Strategy '{}' yielded no recommendations for order {}",
                            activeStrategy.strategyName(), order.getId());
                    continue;
                }

                RoutingRecommendation best = recommendations.get(0);
                ReassignmentSuggestion suggestion = new ReassignmentSuggestion(
                        order.getId(),
                        best.agentId(),
                        best.confidence(),
                        best.reasoning(),
                        SuggestionStatus.PENDING,
                        TriggerReason.AGENT_OFFLINE,
                        activeStrategy.strategyName()
                );
                suggestion.setCreatedAt(Instant.now());

                // Idempotency Layer 2: Database constraint catch for concurrent races
                try {
                    suggestionRepository.save(suggestion);
                    log.info("[Async Re-plan] Generated suggestion for order {} -> recommended agent {} (confidence: {})",
                            order.getId(), best.agentId(), best.confidence());
                } catch (DataIntegrityViolationException dive) {
                    log.info("[Async Re-plan] Idempotency: duplicate suggestion caught by DB constraint for order {}",
                            order.getId());
                }
            }
        } catch (Exception e) {
            log.error("[Async Re-plan] Error processing AgentOfflineEvent for agent {}: {}",
                    event.agentId(), e.getMessage(), e);
        }
    }
}
