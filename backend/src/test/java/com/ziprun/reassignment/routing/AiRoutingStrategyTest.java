package com.ziprun.reassignment.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziprun.reassignment.ai.LlmGateway;
import com.ziprun.reassignment.ai.PromptBuilder;
import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.model.Order;
import com.ziprun.reassignment.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRoutingStrategyTest {

    @Mock
    private LlmGateway llmGateway;

    private PromptBuilder promptBuilder;
    private RuleBasedStrategy ruleBasedStrategy;
    private ObjectMapper objectMapper;
    private AiRoutingStrategy aiRoutingStrategy;

    private Order testOrder;
    private Agent agent1;
    private Agent agent2;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
        ruleBasedStrategy = new RuleBasedStrategy();
        objectMapper = new ObjectMapper();

        aiRoutingStrategy = new AiRoutingStrategy(
                llmGateway,
                promptBuilder,
                ruleBasedStrategy,
                objectMapper
        );

        testOrder = new Order("ORD-001", "Grocery delivery", "AGT-001", OrderStatus.ASSIGNED);
        agent1 = new Agent("AGT-002", "Rahul Verma", 1, AgentStatus.AVAILABLE, null, null);
        agent2 = new Agent("AGT-003", "Ananya Iyer", 3, AgentStatus.BUSY, null, null);
    }

    @Test
    @DisplayName("Should parse valid LLM JSON response and return recommendation")
    void shouldReturnAiRecommendationOnValidJson() {
        String llmJson = "{\"agentId\": \"AGT-002\", \"confidence\": 0.88, \"reasoning\": \"Rahul has low load and is available.\"}";
        when(llmGateway.generateCompletion(anyString())).thenReturn(llmJson);

        List<RoutingRecommendation> results = aiRoutingStrategy.recommend(
                testOrder, List.of(agent1, agent2), TriggerContext.initial());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("AGT-002", results.get(0).agentId());
        assertEquals(0.88, results.get(0).confidence());
        assertEquals("Rahul has low load and is available.", results.get(0).reasoning());
        verify(llmGateway, times(1)).generateCompletion(anyString());
    }

    @Test
    @DisplayName("Should extract JSON wrapped in markdown code fences")
    void shouldExtractJsonFromMarkdownCodeFences() {
        String markdownJson = "```json\n{\"agentId\": \"AGT-003\", \"confidence\": 0.75, \"reasoning\": \"Ananya is nearby.\"}\n```";
        when(llmGateway.generateCompletion(anyString())).thenReturn(markdownJson);

        List<RoutingRecommendation> results = aiRoutingStrategy.recommend(
                testOrder, List.of(agent1, agent2), TriggerContext.initial());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("AGT-003", results.get(0).agentId());
        assertEquals(0.75, results.get(0).confidence());
    }

    @Test
    @DisplayName("Should fall back to rule-based strategy when LLM hallucinates non-existent agent ID")
    void shouldFallbackWhenLlmHallucinatesAgentId() {
        String hallucinatedJson = "{\"agentId\": \"AGT-999\", \"confidence\": 0.99, \"reasoning\": \"Imaginary super agent.\"}";
        when(llmGateway.generateCompletion(anyString())).thenReturn(hallucinatedJson);

        List<RoutingRecommendation> results = aiRoutingStrategy.recommend(
                testOrder, List.of(agent1, agent2), TriggerContext.initial());

        assertNotNull(results);
        assertEquals(1, results.size());
        // RuleBasedStrategy chooses agent with lowest load (agent1: AGT-002 with 1 order vs agent2 with 3)
        assertEquals("AGT-002", results.get(0).agentId());
        assertEquals(0.7, results.get(0).confidence());
        assertTrue(results.get(0).reasoning().contains("lowest active load"));
    }

    @Test
    @DisplayName("Should fall back to rule-based strategy on LLM network timeout or exception")
    void shouldFallbackOnLlmException() {
        when(llmGateway.generateCompletion(anyString())).thenThrow(new RuntimeException("Connection timed out"));

        List<RoutingRecommendation> results = aiRoutingStrategy.recommend(
                testOrder, List.of(agent1, agent2), TriggerContext.initial());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("AGT-002", results.get(0).agentId());
        assertEquals(0.7, results.get(0).confidence());
        assertTrue(results.get(0).reasoning().contains("lowest active load"));
    }

    @Test
    @DisplayName("Should fall back to rule-based strategy on malformed non-JSON output")
    void shouldFallbackOnMalformedOutput() {
        when(llmGateway.generateCompletion(anyString())).thenReturn("I recommend assigning to Rahul Verma.");

        List<RoutingRecommendation> results = aiRoutingStrategy.recommend(
                testOrder, List.of(agent1, agent2), TriggerContext.initial());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("AGT-002", results.get(0).agentId());
        assertEquals(0.7, results.get(0).confidence());
    }
}
