package com.ziprun.reassignment.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "reassignment_suggestions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_pending_replan_per_order",
            columnNames = {"order_id", "trigger_reason", "status"}
        )
    }
)
public class ReassignmentSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 32)
    private String orderId;

    @Column(name = "recommended_agent_id", nullable = false, length = 32)
    private String recommendedAgentId;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "reasoning", columnDefinition = "TEXT", nullable = false)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 30)
    private TriggerReason triggerReason;

    @Column(name = "strategy_used", nullable = false, length = 50)
    private String strategyUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ReassignmentSuggestion() {
    }

    public ReassignmentSuggestion(String orderId, String recommendedAgentId, double confidence,
                                  String reasoning, SuggestionStatus status, TriggerReason triggerReason,
                                  String strategyUsed) {
        this.orderId = orderId;
        this.recommendedAgentId = recommendedAgentId;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.status = status;
        this.triggerReason = triggerReason;
        this.strategyUsed = strategyUsed;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRecommendedAgentId() {
        return recommendedAgentId;
    }

    public void setRecommendedAgentId(String recommendedAgentId) {
        this.recommendedAgentId = recommendedAgentId;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(TriggerReason triggerReason) {
        this.triggerReason = triggerReason;
    }

    public String getStrategyUsed() {
        return strategyUsed;
    }

    public void setStrategyUsed(String strategyUsed) {
        this.strategyUsed = strategyUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ReassignmentSuggestion{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", recommendedAgentId='" + recommendedAgentId + '\'' +
                ", confidence=" + confidence +
                ", status=" + status +
                ", triggerReason=" + triggerReason +
                ", strategyUsed='" + strategyUsed + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
