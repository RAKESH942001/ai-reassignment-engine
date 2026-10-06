package com.ziprun.reassignment.routing;

/**
 * Standard routing recommendation produced by any RoutingStrategy.
 *
 * @param agentId    ID of recommended agent (e.g. AGT-002)
 * @param confidence Confidence score between 0.0 and 1.0
 * @param reasoning  Plain-English explanation for operations personnel
 */
public record RoutingRecommendation(
        String agentId,
        double confidence,
        String reasoning
) {}
