import React from 'react';
import SuggestionCard from './SuggestionCard';

export default function SuggestionQueue({
  suggestions,
  agentsMap,
  ordersMap,
  onAccept,
  onReject,
  processingId
}) {
  const pendingSuggestions = suggestions.filter((s) => s.status === 'PENDING');

  return (
    <section className="dashboard-section" id="suggestion-queue-section">
      <div className="section-header">
        <div className="section-title-wrap">
          <h2 className="section-title">Pending Reassignment Suggestions</h2>
          <span className={`badge ${pendingSuggestions.length > 0 ? 'badge-alert' : 'badge-neutral'}`}>
            {pendingSuggestions.length} Awaiting Ops Approval
          </span>
        </div>
        <p className="section-desc">
          Autonomous suggestions generated via agentic loop or manual re-plan. Human checkpoint required prior to reassignment.
        </p>
      </div>

      {pendingSuggestions.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">✓</div>
          <h4 className="empty-title">All Fleets Running Smoothly</h4>
          <p className="empty-text">
            No orders currently pending reassignment. When an agent goes <strong>OFFLINE</strong>,
            the background agentic loop will automatically generate recommendations here.
          </p>
        </div>
      ) : (
        <div className="suggestions-list">
          {pendingSuggestions.map((s) => (
            <SuggestionCard
              key={s.id}
              suggestion={s}
              agentsMap={agentsMap}
              ordersMap={ordersMap}
              onAccept={onAccept}
              onReject={onReject}
              isProcessing={processingId === s.id}
            />
          ))}
        </div>
      )}
    </section>
  );
}
