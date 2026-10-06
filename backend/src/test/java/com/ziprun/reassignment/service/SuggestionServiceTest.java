package com.ziprun.reassignment.service;

import com.ziprun.reassignment.model.*;
import com.ziprun.reassignment.repository.AgentRepository;
import com.ziprun.reassignment.repository.OrderRepository;
import com.ziprun.reassignment.repository.SuggestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock
    private SuggestionRepository suggestionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private SuggestionService suggestionService;

    @Test
    @DisplayName("acceptSuggestion throws IllegalStateException when recommended agent is currently OFFLINE")
    void testAcceptSuggestion_ThrowsWhenAgentIsOffline() {
        UUID suggestionId = UUID.randomUUID();
        ReassignmentSuggestion suggestion = new ReassignmentSuggestion(
                "ORD-001", "AGT-004", 0.85, "Optimized",
                SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE, "ai"
        );
        suggestion.setId(suggestionId);

        Order order = new Order("ORD-001", "Package", "AGT-001", OrderStatus.REASSIGNMENT_PENDING);

        Agent offlineAgent = new Agent();
        offlineAgent.setId("AGT-004");
        offlineAgent.setName("Kiran Nair");
        offlineAgent.setStatus(AgentStatus.OFFLINE);
        offlineAgent.setActiveOrderCount(0);

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));
        when(orderRepository.findById("ORD-001")).thenReturn(Optional.of(order));
        when(agentRepository.findById("AGT-004")).thenReturn(Optional.of(offlineAgent));

        assertThatThrownBy(() -> suggestionService.acceptSuggestion(suggestionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is currently OFFLINE");

        verify(orderRepository, never()).save(any());
    }
}
