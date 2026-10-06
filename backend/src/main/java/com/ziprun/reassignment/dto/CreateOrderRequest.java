package com.ziprun.reassignment.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateOrderRequest {

    private String id;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Assigned agent ID is required")
    private String assignedAgentId;

    private String pickupZone;
    private String dropoffZone;
    private String weightClass;
    private String priority;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String id, String description, String assignedAgentId) {
        this.id = id;
        this.description = description;
        this.assignedAgentId = assignedAgentId;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
