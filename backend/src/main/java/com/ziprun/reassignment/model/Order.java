package com.ziprun.reassignment.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "assigned_agent_id", length = 32)
    private String assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "pickup_zone")
    private String pickupZone;

    @Column(name = "dropoff_zone")
    private String dropoffZone;

    @Column(name = "weight_class")
    private String weightClass;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "priority")
    private String priority;

    public Order() {
    }

    public Order(String id, String description, String assignedAgentId, OrderStatus status) {
        this.id = id;
        this.description = description;
        this.assignedAgentId = assignedAgentId;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPickupZone() {
        return pickupZone;
    }

    public void setPickupZone(String pickupZone) {
        this.pickupZone = pickupZone;
    }

    public String getDropoffZone() {
        return dropoffZone;
    }

    public void setDropoffZone(String dropoffZone) {
        this.dropoffZone = dropoffZone;
    }

    public String getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(String weightClass) {
        this.weightClass = weightClass;
    }

    public Instant getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(Instant slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", assignedAgentId='" + assignedAgentId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
