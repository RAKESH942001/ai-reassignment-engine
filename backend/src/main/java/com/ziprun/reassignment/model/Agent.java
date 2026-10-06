package com.ziprun.reassignment.model;

import jakarta.persistence.*;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active_order_count", nullable = false)
    private int activeOrderCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentStatus status;

    @Column(name = "current_zone")
    private String currentZone;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    public Agent() {
    }

    public Agent(String id, String name, int activeOrderCount, AgentStatus status, String currentZone, Integer maxCapacity) {
        this.id = id;
        this.name = name;
        this.activeOrderCount = activeOrderCount;
        this.status = status;
        this.currentZone = currentZone;
        this.maxCapacity = maxCapacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(int activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(String currentZone) {
        this.currentZone = currentZone;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", activeOrderCount=" + activeOrderCount +
                ", status=" + status +
                ", currentZone='" + currentZone + '\'' +
                ", maxCapacity=" + maxCapacity +
                '}';
    }
}
