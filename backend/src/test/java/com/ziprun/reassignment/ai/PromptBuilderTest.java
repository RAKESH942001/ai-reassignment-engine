package com.ziprun.reassignment.ai;

import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.model.Order;
import com.ziprun.reassignment.model.OrderStatus;
import com.ziprun.reassignment.routing.TriggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;
    private Order sampleOrder;
    private List<Agent> sampleAgents;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
        sampleOrder = new Order("ORD-001", "Urgent Medicines to HSR Layout", "AGT-001", OrderStatus.ASSIGNED);
        sampleAgents = List.of(
                new Agent("AGT-002", "Rahul Verma", 0, AgentStatus.AVAILABLE, null, null),
                new Agent("AGT-003", "Ananya Iyer", 2, AgentStatus.BUSY, null, null)
        );
    }

    @Test
    @DisplayName("Initial prompt should include order details, assigned agent, and available candidates")
    void shouldBuildInitialPrompt() {
        String prompt = promptBuilder.buildPrompt(sampleOrder, sampleAgents, TriggerContext.initial());

        assertTrue(prompt.contains("ORD-001"));
        assertTrue(prompt.contains("Urgent Medicines to HSR Layout"));
        assertTrue(prompt.contains("AGT-001"));
        assertTrue(prompt.contains("Rahul Verma"));
        assertTrue(prompt.contains("Ananya Iyer"));
        assertTrue(prompt.contains("Respond ONLY with valid JSON"));
        assertFalse(prompt.contains("RECOVERY SITUATION"));
    }

    @Test
    @DisplayName("Re-plan prompt should include recovery framing, offline agent notice, and load balance instruction")
    void shouldBuildReplanPrompt() {
        TriggerContext context = TriggerContext.agentOffline("AGT-001", 3);
        String prompt = promptBuilder.buildPrompt(sampleOrder, sampleAgents, context);

        assertTrue(prompt.contains("RECOVERY SITUATION"));
        assertTrue(prompt.contains("Agent AGT-001 has gone OFFLINE"));
        assertTrue(prompt.contains("stranded 3 order(s)"));
        assertTrue(prompt.contains("distributing load across agents is preferred"));
        assertTrue(prompt.contains("Respond ONLY with valid JSON"));
    }
}
