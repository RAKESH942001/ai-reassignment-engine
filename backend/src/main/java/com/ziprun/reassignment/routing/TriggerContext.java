package com.ziprun.reassignment.routing;

import com.ziprun.reassignment.model.TriggerReason;

/**
 * Context carried into routing decisions.
 *
 * @param reason              Reason for trigger: INITIAL or AGENT_OFFLINE
 * @param offlineAgentId      ID of offline agent if AGENT_OFFLINE, null otherwise
 * @param strandedOrderCount  Number of stranded orders if AGENT_OFFLINE, 0 otherwise
 */
public record TriggerContext(
        TriggerReason reason,
        String offlineAgentId,
        int strandedOrderCount
) {
    public static TriggerContext initial() {
        return new TriggerContext(TriggerReason.INITIAL, null, 0);
    }

    public static TriggerContext agentOffline(String offlineAgentId, int strandedOrderCount) {
        return new TriggerContext(TriggerReason.AGENT_OFFLINE, offlineAgentId, strandedOrderCount);
    }
}
