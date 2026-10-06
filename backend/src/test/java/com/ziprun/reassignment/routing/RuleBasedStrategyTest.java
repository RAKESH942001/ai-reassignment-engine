package com.ziprun.reassignment.routing;

import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.model.Order;
import com.ziprun.reassignment.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedStrategyTest {

    private RuleBasedStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RuleBasedStrategy();
    }

    @Test
    @DisplayName("Should select available agent with lowest active order count")
    void shouldSelectLowestLoadAgent() {
        Agent agent1 = new Agent("AGT-001", "Priya Sharma", 3, AgentStatus.BUSY, null, null);
        Agent agent2 = new Agent("AGT-002", "Rahul Verma", 0, AgentStatus.AVAILABLE, null, null);
        Agent agent3 = new Agent("AGT-003", "Ananya Iyer", 2, AgentStatus.BUSY, null, null);

        Order order = new Order("ORD-001", "Test Order", "AGT-001", OrderStatus.ASSIGNED);
        TriggerContext context = TriggerContext.initial();

        List<RoutingRecommendation> recommendations = strategy.recommend(order, List.of(agent1, agent2, agent3), context);

        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        RoutingRecommendation recommendation = recommendations.get(0);

        assertEquals("AGT-002", recommendation.agentId());
        assertEquals(0.7, recommendation.confidence());
        assertTrue(recommendation.reasoning().contains("lowest active load"));
    }

    @Test
    @DisplayName("Should exclude offline agent specified in TriggerContext during re-planning")
    void shouldExcludeOfflineAgentFromReplan() {
        Agent offlineAgent = new Agent("AGT-001", "Priya Sharma", 0, AgentStatus.OFFLINE, null, null);
        Agent busyAgent = new Agent("AGT-003", "Ananya Iyer", 2, AgentStatus.BUSY, null, null);
        Agent availableAgent = new Agent("AGT-004", "Kiran Nair", 1, AgentStatus.AVAILABLE, null, null);

        Order order = new Order("ORD-001", "Stranded Order", "AGT-001", OrderStatus.REASSIGNMENT_PENDING);
        TriggerContext context = TriggerContext.agentOffline("AGT-001", 3);

        List<RoutingRecommendation> recommendations = strategy.recommend(
                order, List.of(offlineAgent, busyAgent, availableAgent), context);

        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals("AGT-004", recommendations.get(0).agentId());
    }

    @Test
    @DisplayName("RoutingEngine supports runtime switching without restart")
    void shouldSupportRuntimeStrategySwitching() {
        RoutingStrategy aiStub = new RoutingStrategy() {
            @Override
            public List<RoutingRecommendation> recommend(Order order, List<Agent> availableAgents, TriggerContext context) {
                return List.of();
            }

            @Override
            public String strategyName() {
                return "ai";
            }
        };

        RoutingEngine engine = new RoutingEngine(
                Map.of("rule-based", strategy, "ai", aiStub),
                "rule-based"
        );

        assertEquals("rule-based", engine.getActiveStrategyName());
        assertSame(strategy, engine.getActiveStrategy());

        engine.switchStrategy("ai");
        assertEquals("ai", engine.getActiveStrategyName());
        assertSame(aiStub, engine.getActiveStrategy());

        assertThrows(IllegalArgumentException.class, () -> engine.switchStrategy("non-existent-strategy"));
    }
}
