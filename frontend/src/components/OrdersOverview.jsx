import React, { useState } from 'react';

export default function OrdersOverview({ orders, agentsMap, onTriggerSuggest, requestingOrderId }) {
  const [filter, setFilter] = useState('ALL');

  const filteredOrders = orders.filter((o) => {
    if (filter === 'ALL') return true;
    return o.status === filter;
  });

  return (
    <section className="dashboard-section" id="orders-overview-section">
      <div className="section-header">
        <div className="section-title-wrap">
          <h2 className="section-title">Active Orders Overview</h2>
          <span className="badge badge-neutral">{orders.length} Total</span>
        </div>
        <div className="filter-tabs">
          {['ALL', 'REASSIGNMENT_PENDING', 'ASSIGNED', 'REASSIGNED'].map((f) => (
            <button
              key={f}
              type="button"
              className={`filter-tab ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      <div className="orders-table-container">
        <table className="orders-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Description / Route</th>
              <th>Current Assignee</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {filteredOrders.map((order) => {
              const assignedAgent = agentsMap[order.assignedAgentId];
              const isPending = order.status === 'REASSIGNMENT_PENDING';
              const isReassigned = order.status === 'REASSIGNED';
              const isSuggesting = requestingOrderId === order.id;

              let statusBadge = 'status-assigned';
              if (isPending) statusBadge = 'status-pending';
              if (isReassigned) statusBadge = 'status-reassigned';

              return (
                <tr key={order.id} className={isPending ? 'row-pending' : ''}>
                  <td className="font-mono font-bold">{order.id}</td>
                  <td>{order.description}</td>
                  <td>
                    {order.assignedAgentId ? (
                      <span>
                        <strong>{order.assignedAgentId}</strong>
                        {assignedAgent ? ` (${assignedAgent.name})` : ''}
                      </span>
                    ) : (
                      <em className="text-muted">Unassigned</em>
                    )}
                  </td>
                  <td>
                    <span className={`status-pill ${statusBadge}`}>
                      {order.status}
                    </span>
                  </td>
                  <td>
                    <button
                      type="button"
                      disabled={isSuggesting || isPending}
                      className="btn-sm btn-suggest"
                      onClick={() => onTriggerSuggest(order.id)}
                      title="Trigger on-demand suggestion for this order"
                    >
                      {isSuggesting ? 'Thinking...' : '⚡ Suggest Reassignment'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
