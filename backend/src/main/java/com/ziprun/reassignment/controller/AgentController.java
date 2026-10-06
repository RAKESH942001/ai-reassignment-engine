package com.ziprun.reassignment.controller;

import com.ziprun.reassignment.dto.UpdateAgentStatusRequest;
import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgentById(@PathVariable("id") String id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agent> updateAgentStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateAgentStatusRequest request) {
        Agent updated = agentService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
