package com.ziprun.reassignment.dto;

import jakarta.validation.constraints.NotBlank;

public class SwitchStrategyRequest {

    @NotBlank(message = "Strategy name is required (e.g. 'rule-based', 'ai')")
    private String strategy;

    public SwitchStrategyRequest() {
    }

    public SwitchStrategyRequest(String strategy) {
        this.strategy = strategy;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
