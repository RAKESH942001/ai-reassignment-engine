package com.ziprun.reassignment.model;

/**
 * Trigger reason for reassignment suggestions.
 * INITIAL: On-demand operator or system request.
 * AGENT_OFFLINE: Automatically triggered by the agentic re-planning loop when an agent goes offline.
 */
public enum TriggerReason {
    INITIAL,
    AGENT_OFFLINE
}
