-- ─────────────────────────────────────────────────────────
-- Hackathon · Seed Data
-- PostgreSQL compatible · ON CONFLICT (id) DO NOTHING to survive restarts
-- ─────────────────────────────────────────────────────────

-- 5 agents: mix of AVAILABLE and BUSY
INSERT INTO agents (id, name, active_order_count, status, current_zone, max_capacity)
VALUES
    ('AGT-001', 'Priya Sharma',  3, 'BUSY',      NULL, NULL),
    ('AGT-002', 'Rahul Verma',   0, 'AVAILABLE',  NULL, NULL),
    ('AGT-003', 'Ananya Iyer',   2, 'BUSY',      NULL, NULL),
    ('AGT-004', 'Kiran Nair',    0, 'AVAILABLE',  NULL, NULL),
    ('AGT-005', 'Deepak Mehta',  3, 'BUSY',      NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- 8 orders: all ASSIGNED, spread across agents
-- Counts match: AGT-001=3, AGT-003=2, AGT-005=3
INSERT INTO orders (id, description, assigned_agent_id, status, created_at, updated_at,
                   pickup_zone, dropoff_zone, weight_class, sla_deadline, priority)
VALUES
    ('ORD-001', 'Electronics — Koramangala to Indiranagar',   'AGT-001', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-002', 'Groceries — HSR Layout to BTM',              'AGT-001', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-003', 'Pharma — Whitefield to Marathahalli',         'AGT-003', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-004', 'Documents — MG Road to Jayanagar',            'AGT-005', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-005', 'Food — Bellandur to Electronic City',         'AGT-005', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-006', 'Apparel — Malleshwaram to Rajajinagar',       'AGT-005', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-007', 'Books — Banashankari to JP Nagar',            'AGT-003', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL),
    ('ORD-008', 'Hardware — Peenya to Yeshwanthpur',           'AGT-001', 'ASSIGNED', NOW(), NOW(), NULL, NULL, NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;
