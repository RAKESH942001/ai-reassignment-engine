package com.ziprun.reassignment.routing;

import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.Order;

import java.util.List;

/**
 * Strategy interface for assigning/reassigning orders to agents.
 * Implemented by RuleBasedStrategy (Phase 1) and AiRoutingStrategy (Phase 2).
 */
public interface RoutingStrategy {

    /**
     * Recommend agents for the specified order given the available agents and context.
     *
     * @param order           The order requiring reassignment
     * @param availableAgents The currently candidate agents
     * @param context         Trigger context (INITIAL or AGENT_OFFLINE)
     * @return List of recommendations ordered by confidence descending
     */
    List<RoutingRecommendation> recommend(Order order, List<Agent> availableAgents, TriggerContext context);

    /**
     * Unique identifier matching bean name (e.g., "rule-based", "ai").
     */
    String strategyName();
}
