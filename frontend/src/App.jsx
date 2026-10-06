import React, { useState, useEffect, useCallback, useMemo } from 'react';
import Header from './components/Header';
import SuggestionQueue from './components/SuggestionQueue';
import AgentList from './components/AgentList';
import OrdersOverview from './components/OrdersOverview';
import { api } from './api/client';

export default function App() {
  const [agents, setAgents] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [orders, setOrders] = useState([]);
  const [activeStrategy, setActiveStrategy] = useState('rule-based');
  const [availableStrategies, setAvailableStrategies] = useState(['rule-based', 'ai']);

  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const [loadingAgentId, setLoadingAgentId] = useState(null);
  const [processingSuggestionId, setProcessingSuggestionId] = useState(null);
  const [requestingOrderId, setRequestingOrderId] = useState(null);

  // Fast lookups
  const agentsMap = useMemo(() => {
    return agents.reduce((acc, a) => {
      acc[a.id] = a;
      return acc;
    }, {});
  }, [agents]);

  const ordersMap = useMemo(() => {
    return orders.reduce((acc, o) => {
      acc[o.id] = o;
      return acc;
    }, {});
  }, [orders]);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => {
      setNotification((curr) => (curr?.message === message ? null : curr));
    }, 4500);
  };

  // Fetch all dashboard data
  const fetchData = useCallback(async (isManual = false) => {
    if (isManual) setIsRefreshing(true);
    try {
      const [agentsData, suggestionsData, ordersData, strategyData] = await Promise.all([
        api.getAgents(),
        api.getSuggestions('PENDING'),
        api.getOrders(),
        api.getStrategy()
      ]);

      setAgents(agentsData || []);
      setSuggestions(suggestionsData || []);
      setOrders(ordersData || []);
      if (strategyData?.activeStrategy) {
        setActiveStrategy(strategyData.activeStrategy);
      }
      if (strategyData?.availableStrategies) {
        setAvailableStrategies(strategyData.availableStrategies);
      }
      setError(null);
      setLastUpdated(new Date());
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
      setError(err.message || 'Error connecting to backend API');
    } finally {
      setLoading(false);
      if (isManual) setIsRefreshing(false);
    }
  }, []);

  // 5-second polling interval
  useEffect(() => {
    fetchData();
    const interval = setInterval(() => {
      fetchData(false);
    }, 5000);
    return () => clearInterval(interval);
  }, [fetchData]);

  // Strategy switch handler
  const handleSwitchStrategy = async (strategyName) => {
    try {
      const res = await api.switchStrategy(strategyName);
      setActiveStrategy(res.activeStrategy);
      showNotification(`Switched active strategy to ${res.activeStrategy.toUpperCase()}`, 'info');
    } catch (err) {
      showNotification(`Failed to switch strategy: ${err.message}`, 'error');
    }
  };

  // Agent status change (e.g., mark OFFLINE to trigger agentic loop)
  const handleUpdateAgentStatus = async (agentId, newStatus) => {
    setLoadingAgentId(agentId);
    try {
      await api.updateAgentStatus(agentId, newStatus);
      showNotification(`Agent ${agentId} status changed to ${newStatus}. ${newStatus === 'OFFLINE' ? 'Async re-planning loop triggered!' : ''}`, 'info');
      await fetchData();
    } catch (err) {
      showNotification(`Failed to update agent ${agentId}: ${err.message}`, 'error');
    } finally {
      setLoadingAgentId(null);
    }
  };

  // Accept suggestion handler
  const handleAcceptSuggestion = async (suggestionId) => {
    setProcessingSuggestionId(suggestionId);
    try {
      const updated = await api.updateSuggestionStatus(suggestionId, 'ACCEPTED');
      showNotification(`Suggestion accepted! Order ${updated.orderId} reassigned to ${updated.recommendedAgentId}`, 'success');
      await fetchData();
    } catch (err) {
      showNotification(`Failed to accept suggestion: ${err.message}`, 'error');
    } finally {
      setProcessingSuggestionId(null);
    }
  };

  // Reject suggestion handler
  const handleRejectSuggestion = async (suggestionId) => {
    setProcessingSuggestionId(suggestionId);
    try {
      const updated = await api.updateSuggestionStatus(suggestionId, 'REJECTED');
      showNotification(`Suggestion for order ${updated.orderId} rejected. Order reverted to ASSIGNED.`, 'info');
      await fetchData();
    } catch (err) {
      showNotification(`Failed to reject suggestion: ${err.message}`, 'error');
    } finally {
      setProcessingSuggestionId(null);
    }
  };

  // On-demand suggest trigger for an order
  const handleTriggerSuggest = async (orderId) => {
    setRequestingOrderId(orderId);
    try {
      const suggestion = await api.suggestReassignment(orderId);
      showNotification(`Generated recommendation for ${orderId} using strategy '${suggestion.strategyUsed}'`, 'success');
      await fetchData();
    } catch (err) {
      showNotification(`Suggestion error: ${err.message}`, 'error');
    } finally {
      setRequestingOrderId(null);
    }
  };

  return (
    <div className="app-container">
      <Header
        activeStrategy={activeStrategy}
        availableStrategies={availableStrategies}
        onSwitchStrategy={handleSwitchStrategy}
        lastUpdated={lastUpdated}
        isRefreshing={isRefreshing}
        onRefresh={() => fetchData(true)}
        pendingCount={suggestions.length}
      />

      {error && (
        <div className="toast-banner toast-error">
          ⚠️ <strong>Connection Error:</strong> {error} — Ensure Spring Boot is running on port 8080.
        </div>
      )}

      {notification && (
        <div className={`toast-banner toast-${notification.type}`}>
          {notification.message}
        </div>
      )}

      {loading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Connecting to ZipRun Reassignment Engine...</p>
        </div>
      ) : (
        <main className="dashboard-grid">
          {/* Top Priority: Human-in-the-loop Suggestion Queue Floor */}
          <SuggestionQueue
            suggestions={suggestions}
            agentsMap={agentsMap}
            ordersMap={ordersMap}
            onAccept={handleAcceptSuggestion}
            onReject={handleRejectSuggestion}
            processingId={processingSuggestionId}
          />

          {/* Fleet Roster with Direct OFFLINE trigger for demo */}
          <AgentList
            agents={agents}
            onUpdateStatus={handleUpdateAgentStatus}
            loadingAgentId={loadingAgentId}
          />

          {/* Orders Lifecycle tracking table */}
          <OrdersOverview
            orders={orders}
            agentsMap={agentsMap}
            onTriggerSuggest={handleTriggerSuggest}
            requestingOrderId={requestingOrderId}
          />
        </main>
      )}
    </div>
  );
}
