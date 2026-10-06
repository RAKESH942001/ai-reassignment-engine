package com.ziprun.reassignment.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrator service for strategy selection and runtime switching.
 * Holds all discovered RoutingStrategy beans and uses AtomicReference for thread-safe runtime switching without restart.
 */
@Service
public class RoutingEngine {

    private static final Logger log = LoggerFactory.getLogger(RoutingEngine.class);

    private final Map<String, RoutingStrategy> strategies;
    private final AtomicReference<String> activeStrategyName;

    public RoutingEngine(Map<String, RoutingStrategy> strategies,
                         @Value("${routing.strategy:rule-based}") String defaultStrategy) {
        this.strategies = strategies;
        this.activeStrategyName = new AtomicReference<>(defaultStrategy);

        log.info("Initialized RoutingEngine. Active strategy: '{}'. Registered strategies: {}",
                defaultStrategy, strategies.keySet());
    }

    /**
     * Get the currently active routing strategy.
     * Falls back to rule-based if the configured strategy bean is absent.
     */
    public RoutingStrategy getActiveStrategy() {
        String currentName = activeStrategyName.get();
        RoutingStrategy strategy = strategies.get(currentName);
        if (strategy == null) {
            log.warn("Active strategy '{}' not registered as a bean, falling back to 'rule-based'", currentName);
            strategy = strategies.get("rule-based");
            if (strategy == null && !strategies.isEmpty()) {
                strategy = strategies.values().iterator().next();
            }
        }
        return strategy;
    }

    public String getActiveStrategyName() {
        return activeStrategyName.get();
    }

    public Set<String> getAvailableStrategies() {
        return strategies.keySet();
    }

    /**
     * Runtime strategy switch without server restart.
     */
    public void switchStrategy(String newStrategy) {
        if (newStrategy == null || newStrategy.isBlank()) {
            throw new IllegalArgumentException("Strategy name cannot be null or empty");
        }
        String normalized = newStrategy.trim().toLowerCase();
        if (!strategies.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown strategy: '" + newStrategy
                    + "'. Available strategies: " + strategies.keySet());
        }
        activeStrategyName.set(normalized);
        log.info("Switched active routing strategy to: '{}'", normalized);
    }
}
