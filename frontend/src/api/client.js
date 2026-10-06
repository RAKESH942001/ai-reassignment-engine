const API_BASE = 'http://localhost:8080/api';

async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  try {
    const res = await fetch(url, { ...options, headers });
    if (!res.ok) {
      const errorBody = await res.json().catch(() => ({}));
      const message = errorBody.message || `Request failed with status ${res.status}`;
      throw new Error(message);
    }
    return await res.json();
  } catch (err) {
    console.error(`API Error on ${options.method || 'GET'} ${url}:`, err);
    throw err;
  }
}

export const api = {
  // Agents
  getAgents: () => request('/agents'),
  updateAgentStatus: (agentId, status) =>
    request(`/agents/${agentId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    }),

  // Suggestions
  getSuggestions: (status) =>
    request(status ? `/suggestions?status=${status}` : '/suggestions'),
  updateSuggestionStatus: (suggestionId, status) =>
    request(`/suggestions/${suggestionId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    }),

  // Orders
  getOrders: (status) =>
    request(status ? `/orders?status=${status}` : '/orders'),
  suggestReassignment: (orderId) =>
    request(`/orders/${orderId}/suggest`, {
      method: 'POST'
    }),

  // Routing Strategy
  getStrategy: () => request('/routing/strategy'),
  switchStrategy: (strategy) =>
    request('/routing/strategy', {
      method: 'PATCH',
      body: JSON.stringify({ strategy })
    })
};
