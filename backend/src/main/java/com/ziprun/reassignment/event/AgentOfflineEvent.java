package com.ziprun.reassignment.event;

/**
 * Domain event published when an agent transition to OFFLINE status occurs.
 * Decouples the PATCH /api/agents/{id}/status response from the asynchronous re-planning loop.
 */
public record AgentOfflineEvent(
        String agentId,
        String agentName
) {}
