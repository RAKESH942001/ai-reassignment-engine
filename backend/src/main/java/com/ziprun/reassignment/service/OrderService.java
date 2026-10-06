package com.ziprun.reassignment.service;

import com.ziprun.reassignment.dto.CreateOrderRequest;
import com.ziprun.reassignment.exception.ResourceNotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;
    private final SuggestionRepository suggestionRepository;
    private final RoutingEngine routingEngine;

    public OrderService(OrderRepository orderRepository,
                        AgentRepository agentRepository,
                        SuggestionRepository suggestionRepository,
                        RoutingEngine routingEngine) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.suggestionRepository = suggestionRepository;
        this.routingEngine = routingEngine;
    }

    public List<Order> getOrders(OrderStatus status) {
        if (status != null) {
            return orderRepository.findByStatus(status);
        }
        return orderRepository.findAll();
    }

    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order '" + id + "' not found"));
    }

    @Transactional
    public Order createOrder(CreateOrderRequest req) {
        Agent agent = agentRepository.findById(req.getAssignedAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned agent '" + req.getAssignedAgentId() + "' not found"));

        String orderId = (req.getId() != null && !req.getId().isBlank())
                ? req.getId().trim()
                : "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order();
        order.setId(orderId);
        order.setDescription(req.getDescription());
        order.setAssignedAgentId(agent.getId());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setPickupZone(req.getPickupZone());
        order.setDropoffZone(req.getDropoffZone());
        order.setWeightClass(req.getWeightClass());
        order.setPriority(req.getPriority());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        agent.setActiveOrderCount(agent.getActiveOrderCount() + 1);
        agentRepository.save(agent);

        Order saved = orderRepository.save(order);
        log.info("Created order {} assigned to agent {}", saved.getId(), agent.getId());
        return saved;
    }

    /**
     * On-demand reassignment suggestion (POST /api/orders/{id}/suggest).
     */
    @Transactional
    public ReassignmentSuggestion createSuggestionForOrder(String orderId) {
        Order order = getOrderById(orderId);

        // Transition order status to REASSIGNMENT_PENDING
        order.setStatus(OrderStatus.REASSIGNMENT_PENDING);
        orderRepository.save(order);

        List<Agent> availableAgents = agentRepository.findByStatusIn(List.of(AgentStatus.AVAILABLE, AgentStatus.BUSY));
        TriggerContext context = TriggerContext.initial();
        RoutingStrategy strategy = routingEngine.getActiveStrategy();

        List<RoutingRecommendation> recommendations = strategy.recommend(order, availableAgents, context);
        if (recommendations.isEmpty()) {
            throw new IllegalStateException("No candidate agents available for reassignment");
        }

        RoutingRecommendation best = recommendations.get(0);
        ReassignmentSuggestion suggestion = new ReassignmentSuggestion(
                order.getId(),
                best.agentId(),
                best.confidence(),
                best.reasoning(),
                SuggestionStatus.PENDING,
                TriggerReason.INITIAL,
                strategy.strategyName()
        );
        suggestion.setCreatedAt(Instant.now());

        ReassignmentSuggestion saved = suggestionRepository.save(suggestion);
        log.info("Created on-demand suggestion {} for order {} with strategy '{}'",
                saved.getId(), order.getId(), strategy.strategyName());
        return saved;
    }
}
