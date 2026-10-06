package com.ziprun.reassignment.controller;

import com.ziprun.reassignment.dto.SwitchStrategyRequest;
import com.ziprun.reassignment.routing.RoutingEngine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingEngine routingEngine;

    public RoutingController(RoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }

    @GetMapping("/strategy")
    public ResponseEntity<Map<String, Object>> getActiveStrategy() {
        return ResponseEntity.ok(Map.of(
                "activeStrategy", routingEngine.getActiveStrategyName(),
                "availableStrategies", routingEngine.getAvailableStrategies()
        ));
    }

    @PatchMapping("/strategy")
    public ResponseEntity<Map<String, Object>> switchStrategy(@Valid @RequestBody SwitchStrategyRequest request) {
        routingEngine.switchStrategy(request.getStrategy());
        return ResponseEntity.ok(Map.of(
                "activeStrategy", routingEngine.getActiveStrategyName(),
                "availableStrategies", routingEngine.getAvailableStrategies()
        ));
    }
}
