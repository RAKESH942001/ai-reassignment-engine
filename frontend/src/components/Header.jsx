import React from 'react';

export default function Header({
  activeStrategy,
  availableStrategies,
  onSwitchStrategy,
  lastUpdated,
  isRefreshing,
  onRefresh,
  pendingCount
}) {
  return (
    <header className="app-header">
      <div className="header-brand">
        <div className="brand-logo">ZR</div>
        <div>
          <h1 className="brand-title">ZipRun · Ops Dashboard</h1>
          <p className="brand-subtitle">AI Fleet Reassignment & Contingency Engine</p>
        </div>
      </div>

      <div className="header-controls">
        {/* Strategy Switcher */}
        <div className="control-group">
          <label className="control-label" htmlFor="strategy-select">Active Strategy:</label>
          <div className="strategy-selector">
            {availableStrategies.map((s) => (
              <button
                key={s}
                type="button"
                className={`strategy-btn ${activeStrategy === s ? 'active' : ''} ${s === 'ai' ? 'ai-glow' : ''}`}
                onClick={() => onSwitchStrategy(s)}
                id={`strategy-toggle-${s}`}
              >
                {s === 'ai' ? '🤖 AI (qwen-cursor)' : '⚖️ Rule-Based'}
              </button>
            ))}
          </div>
        </div>

        {/* Polling / Refresh */}
        <div className="refresh-box">
          <button
            type="button"
            className={`btn-refresh ${isRefreshing ? 'spinning' : ''}`}
            onClick={onRefresh}
            title="Poll backend now"
            id="refresh-btn"
          >
            ↻ Refresh
          </button>
          <span className="last-sync">
            {lastUpdated ? `Sync: ${lastUpdated.toLocaleTimeString()}` : 'Connecting...'}
          </span>
        </div>
      </div>
    </header>
  );
}
