package com.ziprun.reassignment.dto;

import com.ziprun.reassignment.model.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateSuggestionStatusRequest {

    @NotNull(message = "Suggestion status is required (ACCEPTED or REJECTED)")
    private SuggestionStatus status;

    public UpdateSuggestionStatusRequest() {
    }

    public UpdateSuggestionStatusRequest(SuggestionStatus status) {
        this.status = status;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }
}
