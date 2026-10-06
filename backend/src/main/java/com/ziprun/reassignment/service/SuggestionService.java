package com.ziprun.reassignment.service;

import com.ziprun.reassignment.exception.ResourceNotFoundException;
import com.ziprun.reassignment.model.*;
import com.ziprun.reassignment.repository.AgentRepository;
import com.ziprun.reassignment.repository.OrderRepository;
import com.ziprun.reassignment.repository.SuggestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final SuggestionRepository suggestionRepository;
    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;

    public SuggestionService(SuggestionRepository suggestionRepository,
                             OrderRepository orderRepository,
                             AgentRepository agentRepository) {
        this.suggestionRepository = suggestionRepository;
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
    }

    public List<ReassignmentSuggestion> getSuggestions(SuggestionStatus status) {
        if (status != null) {
            return suggestionRepository.findByStatus(status);
        }
        return suggestionRepository.findAll();
    }

    public ReassignmentSuggestion getSuggestionById(UUID id) {
        return suggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion '" + id + "' not found"));
    }

    /**
     * Process human operator decision (ACCEPTED or REJECTED) on a suggestion.
     */
    @Transactional
    public ReassignmentSuggestion updateSuggestionStatus(UUID id, SuggestionStatus targetStatus) {
        if (targetStatus == SuggestionStatus.ACCEPTED) {
            return acceptSuggestion(id);
        } else if (targetStatus == SuggestionStatus.REJECTED) {
            return rejectSuggestion(id);
        } else {
            throw new IllegalArgumentException("Cannot transition suggestion back to: " + targetStatus);
        }
    }

    /**
     * Full atomic state transition chain for ACCEPT:
     * 1. Suggestion.status → ACCEPTED
     * 2. Order.assignedAgentId → suggestion.recommendedAgentId
     * 3. Order.status → REASSIGNED
     * 4. New Agent.activeOrderCount → +1
     * 5. Old Agent.activeOrderCount → -1 (if old agent exists)
     */
    @Transactional
    public ReassignmentSuggestion acceptSuggestion(UUID suggestionId) {
        ReassignmentSuggestion suggestion = getSuggestionById(suggestionId);
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion '" + suggestionId + "' is already " + suggestion.getStatus());
        }

        Order order = orderRepository.findById(suggestion.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order '" + suggestion.getOrderId() + "' not found"));

        Agent newAgent = agentRepository.findById(suggestion.getRecommendedAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Recommended agent '" + suggestion.getRecommendedAgentId() + "' not found"));

        Agent oldAgent = (order.getAssignedAgentId() != null)
                ? agentRepository.findById(order.getAssignedAgentId()).orElse(null)
                : null;

        // 1. Suggestion.status → ACCEPTED
        suggestion.setStatus(SuggestionStatus.ACCEPTED);

        // 2. Order.assignedAgentId → suggestion.recommendedAgentId
        // 3. Order.status → REASSIGNED
        order.setAssignedAgentId(newAgent.getId());
        order.setStatus(OrderStatus.REASSIGNED);
        orderRepository.save(order);

        // 4. New Agent.activeOrderCount → +1
        newAgent.setActiveOrderCount(newAgent.getActiveOrderCount() + 1);
        agentRepository.save(newAgent);

        // 5. Old Agent.activeOrderCount → -1 (if trackable)
        if (oldAgent != null && oldAgent.getActiveOrderCount() > 0) {
            oldAgent.setActiveOrderCount(oldAgent.getActiveOrderCount() - 1);
            agentRepository.save(oldAgent);
        }

        ReassignmentSuggestion saved = suggestionRepository.save(suggestion);
        log.info("Suggestion {} ACCEPTED: Order {} reassigned to agent {} (previous: {})",
                suggestionId, order.getId(), newAgent.getId(), oldAgent != null ? oldAgent.getId() : "none");
        return saved;
    }

    /**
     * State transition for REJECT:
     * 1. Suggestion.status → REJECTED
     * 2. Order.status → ASSIGNED (reverts back, ready for a new suggestion)
     */
    @Transactional
    public ReassignmentSuggestion rejectSuggestion(UUID suggestionId) {
        ReassignmentSuggestion suggestion = getSuggestionById(suggestionId);
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion '" + suggestionId + "' is already " + suggestion.getStatus());
        }

        Order order = orderRepository.findById(suggestion.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order '" + suggestion.getOrderId() + "' not found"));

        // 1. Suggestion.status → REJECTED
        suggestion.setStatus(SuggestionStatus.REJECTED);

        // 2. Order.status → ASSIGNED (reverts back)
        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);

        ReassignmentSuggestion saved = suggestionRepository.save(suggestion);
        log.info("Suggestion {} REJECTED: Order {} reverted to ASSIGNED", suggestionId, order.getId());
        return saved;
    }
}
