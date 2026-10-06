package com.ziprun.reassignment.dto;

import com.ziprun.reassignment.model.AgentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAgentStatusRequest {

    @NotNull(message = "Agent status is required (AVAILABLE, BUSY, OFFLINE)")
    private AgentStatus status;

    public UpdateAgentStatusRequest() {
    }

    public UpdateAgentStatusRequest(AgentStatus status) {
        this.status = status;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }
}
