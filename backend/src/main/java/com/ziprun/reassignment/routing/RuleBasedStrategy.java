package com.ziprun.reassignment.routing;

import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.model.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Deterministic rule-based strategy.
 * Selects the available agent with the lowest active load.
 * Serves as both an independent strategy and the fallback for LLM failures.
 */
@Component("rule-based")
public class RuleBasedStrategy implements RoutingStrategy {

    public static final String STRATEGY_NAME = "rule-based";
    private static final double DEFAULT_CONFIDENCE = 0.7;

    @Override
    public List<RoutingRecommendation> recommend(Order order, List<Agent> availableAgents, TriggerContext context) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            return List.of();
        }

        List<Agent> eligible = availableAgents.stream()
                // Never assign to OFFLINE agents
                .filter(a -> a.getStatus() != AgentStatus.OFFLINE)
                // Filter out offline agent if specified in context
                .filter(a -> context == null || context.offlineAgentId() == null || !a.getId().equals(context.offlineAgentId()))
                // Filter out current assigned agent to avoid re-assigning to the same agent
                .filter(a -> order == null || order.getAssignedAgentId() == null || !a.getId().equals(order.getAssignedAgentId()))
                // Sort by active order count ASC, then by id for deterministic tie-breaking
                .sorted(Comparator.comparingInt(Agent::getActiveOrderCount).thenComparing(Agent::getId))
                .toList();

        // If filtering out current assigned agent left nothing, but other non-offline agents exist
        if (eligible.isEmpty()) {
            eligible = availableAgents.stream()
                    .filter(a -> a.getStatus() != AgentStatus.OFFLINE)
                    .filter(a -> context == null || context.offlineAgentId() == null || !a.getId().equals(context.offlineAgentId()))
                    .sorted(Comparator.comparingInt(Agent::getActiveOrderCount).thenComparing(Agent::getId))
                    .toList();
        }

        if (eligible.isEmpty()) {
            return List.of();
        }

        Agent best = eligible.get(0);
        String reasoning = String.format("Agent %s (%s) selected: lowest active load (%d orders)",
                best.getName(), best.getId(), best.getActiveOrderCount());

        return List.of(new RoutingRecommendation(best.getId(), DEFAULT_CONFIDENCE, reasoning));
    }

    @Override
    public String strategyName() {
        return STRATEGY_NAME;
    }
}
