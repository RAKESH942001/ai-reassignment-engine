package com.ziprun.reassignment.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziprun.reassignment.ai.LlmGateway;
import com.ziprun.reassignment.ai.LlmResponsePayload;
import com.ziprun.reassignment.ai.PromptBuilder;
import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI-powered routing strategy using OpenAI-compatible chat completions.
 * Features automated rule-based fallback on LLM unavailability, timeout, JSON errors, or hallucinated agent IDs.
 */
@Component("ai")
public class AiRoutingStrategy implements RoutingStrategy {

    public static final String STRATEGY_NAME = "ai";
    private static final Logger log = LoggerFactory.getLogger(AiRoutingStrategy.class);

    private final LlmGateway llmGateway;
    private final PromptBuilder promptBuilder;
    private final RuleBasedStrategy ruleBasedStrategy;
    private final ObjectMapper objectMapper;

    public AiRoutingStrategy(LlmGateway llmGateway,
                              PromptBuilder promptBuilder,
                              RuleBasedStrategy ruleBasedStrategy,
                              ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.promptBuilder = promptBuilder;
        this.ruleBasedStrategy = ruleBasedStrategy;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RoutingRecommendation> recommend(Order order, List<Agent> availableAgents, TriggerContext context) {
        // Prepare eligible candidates (excluding OFFLINE agents and current offline agent if specified)
        List<Agent> candidates = filterCandidates(availableAgents, context);

        if (candidates.isEmpty()) {
            log.warn("No eligible candidate agents available for AI routing, delegating to rule-based");
            return ruleBasedStrategy.recommend(order, availableAgents, context);
        }

        try {
            String prompt = promptBuilder.buildPrompt(order, candidates, context);
            String rawResponse = llmGateway.generateCompletion(prompt);

            String cleanJson = extractJsonContent(rawResponse);
            LlmResponsePayload payload = objectMapper.readValue(cleanJson, LlmResponsePayload.class);

            if (payload == null || payload.agentId() == null || payload.agentId().isBlank()) {
                throw new IllegalStateException("LLM response did not contain a valid agentId");
            }

            // Validate that recommended agent ID is actually an available candidate (hallucination defense)
            validateCandidateAgent(payload.agentId().trim(), candidates);

            double confidence = (payload.confidence() != null)
                    ? Math.max(0.0, Math.min(1.0, payload.confidence()))
                    : 0.85;

            String reasoning = (payload.reasoning() != null && !payload.reasoning().isBlank())
                    ? payload.reasoning().trim()
                    : "Agent selected via AI optimization model";

            log.info("AI routing recommended agent '{}' with confidence {} for order {}",
                    payload.agentId(), confidence, (order != null ? order.getId() : "N/A"));

            return List.of(new RoutingRecommendation(payload.agentId().trim(), confidence, reasoning));

        } catch (Exception ex) {
            log.warn("AI routing strategy failure: [{}]. Falling back to rule-based strategy.", ex.getMessage());
            return ruleBasedStrategy.recommend(order, availableAgents, context);
        }
    }

    @Override
    public String strategyName() {
        return STRATEGY_NAME;
    }

    private List<Agent> filterCandidates(List<Agent> agents, TriggerContext context) {
        if (agents == null) {
            return List.of();
        }
        return agents.stream()
                .filter(a -> a.getStatus() != AgentStatus.OFFLINE)
                .filter(a -> context == null || context.offlineAgentId() == null || !a.getId().equals(context.offlineAgentId()))
                .toList();
    }

    private void validateCandidateAgent(String recommendedAgentId, List<Agent> candidates) {
        Set<String> validIds = candidates.stream()
                .map(Agent::getId)
                .collect(Collectors.toSet());

        if (!validIds.contains(recommendedAgentId)) {
            throw new IllegalArgumentException(String.format(
                    "Hallucinated agent ID '%s' returned by LLM. Valid candidate IDs: %s",
                    recommendedAgentId, validIds));
        }
    }

    private String extractJsonContent(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();
        // Remove markdown code fences if model wrapped response in ```json ... ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        // Extract outermost JSON object { ... } if preamble or suffix exists
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }
}
