package com.ziprun.reassignment.service;

import com.ziprun.reassignment.event.AgentOfflineEvent;
import com.ziprun.reassignment.exception.ResourceNotFoundException;
import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.repository.AgentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AgentService(AgentRepository agentRepository, ApplicationEventPublisher eventPublisher) {
        this.agentRepository = agentRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    public Agent getAgentById(String id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent '" + id + "' not found"));
    }

    @Transactional
    public Agent updateStatus(String id, AgentStatus newStatus) {
        Agent agent = getAgentById(id);
        AgentStatus previousStatus = agent.getStatus();
        agent.setStatus(newStatus);
        Agent updated = agentRepository.save(agent);

        log.info("Agent {} status changed: {} -> {}", id, previousStatus, newStatus);

        // When agent goes OFFLINE, publish domain event to fire async re-planning loop
        if (newStatus == AgentStatus.OFFLINE && previousStatus != AgentStatus.OFFLINE) {
            log.info("Agent {} is now OFFLINE. Publishing AgentOfflineEvent to trigger re-planning loop", id);
            eventPublisher.publishEvent(new AgentOfflineEvent(agent.getId(), agent.getName()));
        }

        return updated;
    }
}
