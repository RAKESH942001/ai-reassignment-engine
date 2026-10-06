import React, { useState } from 'react';

export default function AgentList({ agents, onUpdateStatus, loadingAgentId }) {
  return (
    <section className="dashboard-section" id="agent-roster-section">
      <div className="section-header">
        <div className="section-title-wrap">
          <h2 className="section-title">Delivery Fleet Roster</h2>
          <span className="badge badge-neutral">{agents.length} Agents</span>
        </div>
        <p className="section-desc">
          Monitor agent capacity and simulate breakdown by setting status to <strong>OFFLINE</strong>.
        </p>
      </div>

      <div className="agents-grid">
        {agents.map((agent) => {
          const isBusy = agent.status === 'BUSY';
          const isOffline = agent.status === 'OFFLINE';
          const isAvailable = agent.status === 'AVAILABLE';
          const isUpdating = loadingAgentId === agent.id;

          let statusClass = 'status-available';
          if (isBusy) statusClass = 'status-busy';
          if (isOffline) statusClass = 'status-offline';

          return (
            <div
              key={agent.id}
              className={`agent-card ${isOffline ? 'agent-card-offline' : ''}`}
              id={`agent-card-${agent.id}`}
            >
              <div className="agent-card-header">
                <div>
                  <div className="agent-id-tag">{agent.id}</div>
                  <h3 className="agent-name">{agent.name}</h3>
                </div>
                <span className={`status-pill ${statusClass}`}>
                  {agent.status}
                </span>
              </div>

              <div className="agent-meta">
                <div className="meta-item">
                  <span className="meta-label">Active Orders</span>
                  <span className={`meta-value ${agent.activeOrderCount > 0 ? 'text-accent' : ''}`}>
                    {agent.activeOrderCount}
                  </span>
                </div>
                {agent.currentZone && (
                  <div className="meta-item">
                    <span className="meta-label">Zone</span>
                    <span className="meta-value">{agent.currentZone}</span>
                  </div>
                )}
              </div>

              <div className="agent-actions">
                <label className="action-hint">Change Status:</label>
                <div className="action-buttons">
                  <button
                    type="button"
                    disabled={isUpdating || isAvailable}
                    className={`btn-state btn-avail ${isAvailable ? 'btn-selected' : ''}`}
                    onClick={() => onUpdateStatus(agent.id, 'AVAILABLE')}
                    title="Mark available"
                  >
                    Avail
                  </button>
                  <button
                    type="button"
                    disabled={isUpdating || isBusy}
                    className={`btn-state btn-busy ${isBusy ? 'btn-selected' : ''}`}
                    onClick={() => onUpdateStatus(agent.id, 'BUSY')}
                    title="Mark busy"
                  >
                    Busy
                  </button>
                  <button
                    type="button"
                    disabled={isUpdating || isOffline}
                    className={`btn-state btn-offline ${isOffline ? 'btn-selected' : ''}`}
                    onClick={() => onUpdateStatus(agent.id, 'OFFLINE')}
                    title="Simulate outage / go OFFLINE"
                  >
                    Offline
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
