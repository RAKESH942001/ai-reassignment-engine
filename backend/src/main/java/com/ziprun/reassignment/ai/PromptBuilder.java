package com.ziprun.reassignment.ai;

import com.ziprun.reassignment.model.Agent;
import com.ziprun.reassignment.model.AgentStatus;
import com.ziprun.reassignment.model.Order;
import com.ziprun.reassignment.model.TriggerReason;
import com.ziprun.reassignment.routing.TriggerContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds distinct prompts for initial reassignment requests and emergency agent-offline re-planning loops.
 */
@Component
public class PromptBuilder {

    /**
     * Build prompt based on trigger reason in context.
     */
    public String buildPrompt(Order order, List<Agent> candidateAgents, TriggerContext context) {
        if (context != null && context.reason() == TriggerReason.AGENT_OFFLINE) {
            return buildReplanPrompt(order, candidateAgents, context);
        }
        return buildInitialPrompt(order, candidateAgents);
    }

    /**
     * Prompt 1: Initial on-demand suggestion prompt.
     */
    public String buildInitialPrompt(Order order, List<Agent> candidateAgents) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an operations routing advisor for ZipRun, a delivery fleet.\n\n");
        sb.append("Given the following order and available agents, recommend the best agent for this delivery.\n\n");

        sb.append("ORDER:\n");
        sb.append("- ID: ").append(order.getId()).append("\n");
        sb.append("- Description: ").append(order.getDescription()).append("\n");
        sb.append("- Currently assigned to: ").append(order.getAssignedAgentId() != null ? order.getAssignedAgentId() : "Unassigned")
                .append(" (requesting reassignment)\n\n");

        sb.append("AVAILABLE AGENTS:\n");
        appendAgents(sb, candidateAgents);

        sb.append("\nRespond ONLY with valid JSON (no markdown formatting, no explanation outside JSON):\n");
        sb.append("{\"agentId\": \"<agent_id_from_list>\", \"confidence\": <0.0-1.0>, \"reasoning\": \"<brief plain-English explanation for ops>\"}\n");

        return sb.toString();
    }

    /**
     * Prompt 2: Agent-offline recovery re-plan prompt (genuinely different framing, excluding offline agent).
     */
    public String buildReplanPrompt(Order order, List<Agent> candidateAgents, TriggerContext context) {
        String offlineAgentId = (context != null && context.offlineAgentId() != null) ? context.offlineAgentId() : "Unknown";
        int strandedCount = (context != null) ? context.strandedOrderCount() : 1;

        StringBuilder sb = new StringBuilder();
        sb.append("You are an operations routing advisor for ZipRun, a delivery fleet.\n\n");
        sb.append("⚠️ RECOVERY SITUATION: Agent ").append(offlineAgentId).append(" has gone OFFLINE.\n");
        sb.append("This has stranded ").append(strandedCount).append(" order(s) that were assigned to them.\n\n");

        sb.append("You are now re-planning ONE of those stranded orders. Previous assignment to ")
                .append(offlineAgentId).append(" is VOID.\n\n");

        sb.append("ORDER REQUIRING RE-PLAN:\n");
        sb.append("- ID: ").append(order.getId()).append("\n");
        sb.append("- Description: ").append(order.getDescription()).append("\n");
        sb.append("- Was assigned to: ").append(offlineAgentId).append(" (NOW OFFLINE — do NOT recommend this agent)\n\n");

        sb.append("AVAILABLE AGENTS (excluding offline agent):\n");
        appendAgents(sb, candidateAgents);

        sb.append("\nConsider that other stranded orders from the same offline agent may also need reassignment — ");
        sb.append("distributing load across agents is preferred over overloading a single one.\n\n");

        sb.append("Respond ONLY with valid JSON (no markdown formatting, no explanation outside JSON):\n");
        sb.append("{\"agentId\": \"<agent_id_from_list>\", \"confidence\": <0.0-1.0>, \"reasoning\": \"<brief plain-English explanation for ops>\"}\n");

        return sb.toString();
    }

    private void appendAgents(StringBuilder sb, List<Agent> agents) {
        if (agents == null || agents.isEmpty()) {
            sb.append("- None available\n");
            return;
        }
        for (Agent a : agents) {
            sb.append("- ").append(a.getId())
                    .append(": ").append(a.getName())
                    .append(", current load: ").append(a.getActiveOrderCount()).append(" orders")
                    .append(", status: ").append(a.getStatus())
                    .append("\n");
        }
    }
}
