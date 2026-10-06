package com.ziprun.reassignment.ai;

/**
 * Gateway contract for LLM chat completion invocations.
 */
public interface LlmGateway {

    /**
     * Send prompt to the LLM and return the raw text response content.
     *
     * @param prompt The prompt string
     * @return Raw text content returned by the LLM
     */
    String generateCompletion(String prompt);
}
