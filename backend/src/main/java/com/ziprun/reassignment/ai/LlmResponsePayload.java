package com.ziprun.reassignment.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Expected JSON payload format parsed from LLM content response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmResponsePayload(
        String agentId,
        Double confidence,
        String reasoning
) {}
