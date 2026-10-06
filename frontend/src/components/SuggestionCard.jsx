import React from 'react';

export default function SuggestionCard({
  suggestion,
  agentsMap,
  ordersMap,
  onAccept,
  onReject,
  isProcessing
}) {
  const isAgentOffline = suggestion.triggerReason === 'AGENT_OFFLINE';
  const recommendedAgent = agentsMap[suggestion.recommendedAgentId];
  const order = ordersMap[suggestion.orderId];
  const confidencePercent = Math.round((suggestion.confidence || 0) * 100);

  return (
    <div
      className={`suggestion-card ${isAgentOffline ? 'card-replan-offline' : ''}`}
      id={`suggestion-card-${suggestion.id}`}
    >
      <div className="suggestion-card-header">
        <div className="suggestion-badges">
          {isAgentOffline ? (
            <span className="badge badge-emergency">
              🚨 AGENT OFFLINE RE-PLAN
            </span>
          ) : (
            <span className="badge badge-neutral">
              ⚙️ INITIAL REASSIGNMENT
            </span>
          )}

          <span className={`badge ${suggestion.strategyUsed === 'ai' ? 'badge-ai' : 'badge-rule'}`}>
            {suggestion.strategyUsed === 'ai' ? '🤖 AI Powered' : '⚖️ Rule-Based'}
          </span>
        </div>

        <div className="confidence-meter-wrap">
          <span className="confidence-label">Confidence</span>
          <div className="confidence-pill">
            <div
              className="confidence-fill"
              style={{ width: `${confidencePercent}%` }}
            />
            <span className="confidence-text">{confidencePercent}%</span>
          </div>
        </div>
      </div>

      <div className="suggestion-body">
        <div className="order-context">
          <div className="context-row">
            <span className="context-label">Order:</span>
            <span className="context-val order-code">{suggestion.orderId}</span>
            {order && <span className="order-desc">— {order.description}</span>}
          </div>
          {order && order.assignedAgentId && (
            <div className="context-row">
              <span className="context-label">Previous Assignee:</span>
              <span className="context-val text-muted">
                {order.assignedAgentId}
                {agentsMap[order.assignedAgentId] ? ` (${agentsMap[order.assignedAgentId].name})` : ''}
              </span>
            </div>
          )}
        </div>

        <div className="recommendation-box">
          <div className="rec-header">
            <span className="rec-title">Recommended Agent:</span>
            <span className="rec-agent-id">{suggestion.recommendedAgentId}</span>
            {recommendedAgent && (
              <span className="rec-agent-name">
                {recommendedAgent.name} (Active Orders: {recommendedAgent.activeOrderCount})
              </span>
            )}
          </div>

          <div className="reasoning-container">
            <div className="reasoning-label">Verbatim AI / Strategy Reasoning:</div>
            <blockquote className="reasoning-quote">
              "{suggestion.reasoning}"
            </blockquote>
          </div>
        </div>
      </div>

      <div className="suggestion-actions">
        <button
          type="button"
          disabled={isProcessing}
          className="btn-action btn-accept"
          onClick={() => onAccept(suggestion.id)}
          id={`accept-btn-${suggestion.id}`}
        >
          {isProcessing ? 'Processing...' : '✓ Accept Reassignment'}
        </button>
        <button
          type="button"
          disabled={isProcessing}
          className="btn-action btn-reject"
          onClick={() => onReject(suggestion.id)}
          id={`reject-btn-${suggestion.id}`}
        >
          ✕ Reject
        </button>
      </div>
    </div>
  );
}
