package com.ziprun.reassignment.model;

/**
 * Agent availability states.
 * OFFLINE triggers the agentic re-planning loop.
 */
public enum AgentStatus {
    AVAILABLE,
    BUSY,
    OFFLINE
}
