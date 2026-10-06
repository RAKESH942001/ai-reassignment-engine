# AI Reassignment Engine — Architecture Proposal

> **Goal**: Production-oriented but hackathon-scoped design to maximize the 117-point rubric in 5 hours.

---

## 1. Domain Model & State Machines

### 1.1 Entity: `Order`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` (PK) | e.g. `ORD-001` |
| `description` | `String` | Free text, from seed data |
| `assignedAgentId` | `String` (FK → Agent) | Current assignee |
| `status` | `Enum` | State machine below |
| `createdAt` | `Instant` | Audit |
| `updatedAt` | `Instant` | Audit |
| `pickupZone` | `String` (nullable) | Sprint 2 placeholder |
| `dropoffZone` | `String` (nullable) | Sprint 2 placeholder |
| `weightClass` | `String` (nullable) | Sprint 2 placeholder |
| `slaDeadline` | `Instant` (nullable) | Sprint 3 placeholder |
| `priority` | `String` (nullable) | Sprint 3 placeholder |

**State machine:**
```
ASSIGNED → REASSIGNMENT_PENDING → REASSIGNED → DELIVERED
```

- `ASSIGNED`: Order is live, assigned to an agent.
- `REASSIGNMENT_PENDING`: Agent went offline or manual re-suggest triggered; awaiting ops decision.
- `REASSIGNED`: Ops accepted a suggestion; order is now with the new agent.
- `DELIVERED`: Terminal state (not actively used in sprint 1 but needed for completeness).

> **Scoring link**: Entity design (8 pts) — clean state machine, nullable sprint 2/3 fields show forethought without cost.

### 1.2 Entity: `Agent`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` (PK) | e.g. `AGT-001` |
| `name` | `String` | Display name |
| `activeOrderCount` | `int` | Denormalized for fast routing |
| `status` | `Enum` | `AVAILABLE`, `BUSY`, `OFFLINE` |
| `currentZone` | `String` (nullable) | Sprint 2 placeholder |
| `maxCapacity` | `Integer` (nullable) | Sprint 2 placeholder |

> **Design note**: `activeOrderCount` is denormalized. In sprint 2 with `maxCapacity`, we'd derive it from a count query, but for the hackathon the seed-data-driven count is deterministic and avoids N+1 during routing.

### 1.3 Entity: `ReassignmentSuggestion`

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` (PK) | Auto-generated |
| `orderId` | `String` (FK → Order) | Which order |
| `recommendedAgentId` | `String` (FK → Agent) | AI/rule recommendation |
| `confidence` | `double` | 0.0–1.0 |
| `reasoning` | `String` (TEXT) | LLM plain-English explanation |
| `status` | `Enum` | `PENDING`, `ACCEPTED`, `REJECTED` |
| `triggerReason` | `Enum` | `INITIAL`, `AGENT_OFFLINE` |
| `strategyUsed` | `String` | `"rule-based"` or `"ai"` — for transparency |
| `createdAt` | `Instant` | Audit |

> **Scoring link**: `triggerReason` enables the re-plan badge in UI (agentic loop — checkpoint 8 pts). `strategyUsed` gives ops transparency.

### 1.4 JPA Relationships

```
Agent (1) ←→ (N) Order          via Order.assignedAgentId
Order  (1) ←→ (N) ReassignmentSuggestion  via Suggestion.orderId
Agent  (1) ←→ (N) ReassignmentSuggestion  via Suggestion.recommendedAgentId
```

- All relationships are **unidirectional ManyToOne** from child → parent. No bidirectional collections — avoids N+1 and keeps entities simple.
- Fetch strategies: all `LAZY`. Queries use explicit JPQL joins where needed.

> **Scoring link**: Persistence (5 pts) — no N+1, correct JPA relationships.

---

## 2. REST API Design

| Endpoint | Verb | Purpose | Status Codes | Scoring |
|---|---|---|---|---|
| `/api/orders` | `POST` | Create order pre-assigned to agent | `201 Created` | API correctness (7 pts) |
| `/api/orders?status={status}` | `GET` | List orders, filtered by status | `200 OK` | API correctness |
| `/api/agents/{id}/status` | `PATCH` | Update agent availability; fires agentic loop if OFFLINE | `200 OK` (immediate return) | Agentic loop trigger (7 pts) |
| `/api/suggestions/{id}` | `PATCH` | Accept or reject suggestion `{ "status": "ACCEPTED" }` — **see §2.1 below for full state transition** | `200 OK` | Ops interface floor (12 pts) |
| `/api/orders/{id}/suggest` | `POST` | On-demand: run active routing strategy, persist & return suggestion | `201 Created` | Routing contract (10 pts) |
| `/api/agents` | `GET` | List all agents with status | `200 OK` | UI needs |
| `/api/suggestions?status=PENDING` | `GET` | List pending suggestions (for UI polling) | `200 OK` | UI polling |
| `/api/routing/strategy` | `GET` | Returns current active strategy name | `200 OK` | Runtime switchability (8 pts) |
| `/api/routing/strategy` | `PATCH` | Switch active strategy at runtime `{ "strategy": "ai" }` | `200 OK` | Runtime switchability (8 pts) |

### Error Shape (Consistent)
```json
{
  "timestamp": "2026-10-06T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order ORD-999 not found",
  "path": "/api/orders/ORD-999"
}
```

> **Scoring link**: API correctness (7 pts) — right verbs (PATCH not PUT for partial update), structured errors, validation via `@Valid`.

### 2.1 Accept Suggestion — Full State Transition Chain

When ops calls `PATCH /api/suggestions/{id}` with `{ "status": "ACCEPTED" }`, the following **must all happen atomically in a single `@Transactional` method**:

```
1. Suggestion.status  →  ACCEPTED
2. Order.assignedAgentId  →  suggestion.recommendedAgentId
3. Order.status  →  REASSIGNED
4. New Agent.activeOrderCount  →  +1
5. Old Agent.activeOrderCount  →  -1 (if old agent is still trackable)
```

For `REJECTED`:
```
1. Suggestion.status  →  REJECTED
2. Order.status  →  ASSIGNED  (reverts back, ready for a new suggestion)
```

> **Why this matters**: Without step 2–5, accepting a suggestion doesn't actually reassign the order. The demo path — accept suggestion → order shows new agent — would break.

```java
@Transactional
public ReassignmentSuggestion acceptSuggestion(UUID suggestionId) {
    var suggestion = suggestionRepo.findById(suggestionId).orElseThrow(...);
    suggestion.setStatus(SuggestionStatus.ACCEPTED);
    
    var order = orderRepo.findById(suggestion.getOrderId()).orElseThrow(...);
    var oldAgent = agentRepo.findById(order.getAssignedAgentId()).orElse(null);
    var newAgent = agentRepo.findById(suggestion.getRecommendedAgentId()).orElseThrow(...);
    
    order.setAssignedAgentId(newAgent.getId());
    order.setStatus(OrderStatus.REASSIGNED);
    newAgent.setActiveOrderCount(newAgent.getActiveOrderCount() + 1);
    if (oldAgent != null && oldAgent.getActiveOrderCount() > 0) {
        oldAgent.setActiveOrderCount(oldAgent.getActiveOrderCount() - 1);
    }
    return suggestionRepo.save(suggestion);
}
```

---

## 3. Routing Engine — Strategy Pattern

### 3.1 Interface: `RoutingStrategy`

```java
public interface RoutingStrategy {
    List<RoutingRecommendation> recommend(Order order, List<Agent> availableAgents, TriggerContext context);
    String strategyName();
}
```

```java
public record RoutingRecommendation(String agentId, double confidence, String reasoning) {}
```

```java
public record TriggerContext(TriggerReason reason, String offlineAgentId, int strandedOrderCount) {}
```

> **Key design**: The `TriggerContext` carries whether this is `INITIAL` or `AGENT_OFFLINE`, enabling distinct prompts in the AI strategy. Both HTTP and async callers construct the appropriate context.

### 3.2 Rule-Based Strategy (`rule-based`)

```
Algorithm: Sort available agents by activeOrderCount ASC → return top agent
Confidence: 0.7 (fixed — it's deterministic but simplistic)
Reasoning: "Agent {name} selected: lowest active load ({count} orders)"
```

- Zero external dependencies. Always available. Serves as LLM fallback.

### 3.3 AI Strategy (`ai`)

- Constructs a structured prompt with order details + full agent roster + trigger context
- Calls `LLMGateway.callLLM(prompt)`
- Parses JSON response: `{ "agentId": "...", "confidence": 0.85, "reasoning": "..." }`
- **Validates** `agentId` against actual available agents list
- On any failure → delegates to `RuleBasedStrategy` internally, logs the fallback reason
- Returns `RoutingRecommendation` list

### 3.4 Runtime Strategy Switching

**Mechanism**: Spring auto-wired `Map<String, RoutingStrategy>` bean map + `AtomicReference` for true runtime switching + REST API endpoints.

> [!WARNING]
> **Correction from v1**: `@Value` only reads at startup. Changing an env var does NOT auto-switch without a restart. We use an `AtomicReference<String>` that is mutated via a REST endpoint — this is genuine runtime switching.

```java
@Service
public class RoutingEngine {
    private final Map<String, RoutingStrategy> strategies;
    private final AtomicReference<String> activeStrategyName;

    public RoutingEngine(Map<String, RoutingStrategy> strategies,
                         @Value("${routing.strategy:rule-based}") String defaultStrategy) {
        this.strategies = strategies;
        this.activeStrategyName = new AtomicReference<>(defaultStrategy);
        // Startup validation
        if (!strategies.containsKey(defaultStrategy)) {
            throw new IllegalStateException("Unknown default strategy: " + defaultStrategy);
        }
    }

    public RoutingStrategy getActiveStrategy() {
        return strategies.get(activeStrategyName.get());
    }

    public String getActiveStrategyName() {
        return activeStrategyName.get();
    }

    public void switchStrategy(String newStrategy) {
        if (!strategies.containsKey(newStrategy)) {
            throw new IllegalArgumentException("Unknown strategy: " + newStrategy
                + ". Available: " + strategies.keySet());
        }
        activeStrategyName.set(newStrategy);
    }
}
```

**REST endpoints for switching** (in `RoutingController`):

```java
@RestController
@RequestMapping("/api/routing")
public class RoutingController {
    private final RoutingEngine routingEngine;

    @GetMapping("/strategy")
    public Map<String, String> getStrategy() {
        return Map.of("activeStrategy", routingEngine.getActiveStrategyName());
    }

    @PatchMapping("/strategy")
    public Map<String, String> switchStrategy(@RequestBody Map<String, String> body) {
        String newStrategy = body.get("strategy");
        routingEngine.switchStrategy(newStrategy);
        return Map.of("activeStrategy", routingEngine.getActiveStrategyName());
    }
}
```

**Usage**: `PATCH /api/routing/strategy { "strategy": "ai" }` → switches instantly, no restart.

- **Default**: `routing.strategy=rule-based` in `application.properties` sets the initial strategy on startup.
- **Sprint 2 extensibility**: Adding `ZoneAffinityStrategy` = implement `RoutingStrategy`, annotate `@Component("zone-affinity")`, call `PATCH /api/routing/strategy { "strategy": "zone-affinity" }`. Zero changes to existing code.
- **Both callers** (HTTP endpoint + async event handler) call `routingEngine.getActiveStrategy().recommend(...)`. Thread-safe via `AtomicReference`.

> **Scoring link**: Routing contract (10 pts), runtime switchability (8 pts), pattern justification (7 pts) — this is 25 points total, the highest-weighted task.

---

## 4. AI Integration — Two Prompts

### 4.1 Initial Suggestion Prompt

```
You are an operations routing advisor for ZipRun, a delivery fleet.

Given the following order and available agents, recommend the best agent for this delivery.

ORDER:
- ID: {orderId}
- Description: {description}
- Currently assigned to: {currentAgentId} (requesting reassignment)

AVAILABLE AGENTS:
{foreach agent → "- {id}: {name}, current load: {activeOrderCount} orders, status: {status}"}

Respond ONLY with valid JSON:
{"agentId": "<agent_id_from_list>", "confidence": <0.0-1.0>, "reasoning": "<brief plain-English explanation for ops>"}
```

### 4.2 Re-Plan Prompt (Agent Offline — genuinely different)

```
You are an operations routing advisor for ZipRun, a delivery fleet.

⚠️ RECOVERY SITUATION: Agent {offlineAgentName} ({offlineAgentId}) has gone OFFLINE.
This has stranded {strandedCount} orders that were assigned to them.

You are now re-planning ONE of those stranded orders. Previous assignment to {offlineAgentId} is VOID.

ORDER REQUIRING RE-PLAN:
- ID: {orderId}
- Description: {description}
- Was assigned to: {offlineAgentId} (NOW OFFLINE — do NOT recommend this agent)

AVAILABLE AGENTS (excluding offline agent):
{foreach agent → "- {id}: {name}, current load: {activeOrderCount} orders, status: {status}"}

Consider that other stranded orders from the same offline agent may also need reassignment — 
distributing load across agents is preferred over overloading a single one.

Respond ONLY with valid JSON:
{"agentId": "<agent_id_from_list>", "confidence": <0.0-1.0>, "reasoning": "<brief plain-English explanation for ops>"}
```

> **Scoring link**: Initial prompt quality (8 pts), re-plan prompt quality (8 pts). The re-plan prompt is genuinely different — recovery framing, exclusion of offline agent, awareness of stranded order count.

### 4.3 AI Resilience — Failure Mode Matrix

| Failure Mode | Detection | Response | Scoring |
|---|---|---|---|
| **LLM timeout** | `RestClient` timeout config (5s) | Fall back to rule-based, log `WARN` | AI resilience (9 pts) |
| **HTTP error** (429, 500, etc.) | `RestClientException` | Fall back to rule-based, log `WARN` | AI resilience |
| **Unparseable JSON** | `JsonProcessingException` from ObjectMapper | Fall back to rule-based, log `WARN` with raw response | AI resilience |
| **Hallucinated agent ID** | Check `agentId` against `availableAgents` list | Fall back to rule-based, log `WARN` with hallucinated ID | AI resilience |
| **Quota exhaustion** | 429 status code | Fall back to rule-based, log `ERROR` | AI resilience |
| **Async re-plan AI failure** | Any exception in async handler | **Must still produce a rule-based suggestion** — never silent drop | AI resilience |

```java
// In AiRoutingStrategy.recommend():
try {
    String raw = llmGateway.callLLM(prompt);
    LlmResponse parsed = objectMapper.readValue(raw, LlmResponse.class);
    validateAgentId(parsed.agentId(), availableAgents);
    return List.of(new RoutingRecommendation(parsed.agentId(), parsed.confidence(), parsed.reasoning()));
} catch (Exception e) {
    log.warn("AI strategy failed, falling back to rule-based: {}", e.getMessage());
    return ruleBasedStrategy.recommend(order, availableAgents, context);
}
```

> **Scoring link**: AI resilience (9 pts) — explicit handling for every failure mode, never a silent drop.

---

## 5. Agentic Re-Planning Loop (T-4)

### 5.1 Trigger Mechanism

> [!CAUTION]
> **Critical implementation rule**: The AI/routing call must NEVER execute inside the PATCH request thread. The `@Async` annotation on the `@EventListener` is what enforces this. If `@EnableAsync` is missing from config, Spring will execute the listener synchronously and the PATCH will block until the LLM responds — breaking the requirement.

```
PATCH /agents/{id}/status { "status": "OFFLINE" }
    │
    ├── 1. Update agent status in DB  ──→  Return 200 immediately
    │
    └── 2. Publish AgentOfflineEvent via ApplicationEventPublisher
            │                              (AFTER @Transactional commits)
            │
            └── @Async @EventListener handles AgentOfflineEvent
                    │                  (runs on separate thread pool)
                    │
                    ├── Query: find all orders WHERE assignedAgentId = {id} AND status = ASSIGNED
                    │
                    ├── For each affected order:
                    │     ├── Idempotency check (dual-layer — see §5.3):
                    │     │   ├── App-level: existsByOrderIdAndTriggerReasonAndStatus(...)
                    │     │   └── DB-level: unique constraint catches race conditions
                    │     ├── Transition order status → REASSIGNMENT_PENDING
                    │     ├── Run activeStrategy.recommend(order, availableAgents, replanContext)
                    │     └── Persist ReassignmentSuggestion (triggerReason=AGENT_OFFLINE)
                    │
                    └── If strategy fails → fallback to rule-based (never drop)
```

### 5.2 Domain Event

```java
public record AgentOfflineEvent(String agentId, String agentName) {}
```

Published by the agent status update service method, consumed by an `@Async @EventListener`. This decouples the PATCH response from the re-plan work.

### 5.3 Idempotency — Dual-Layer Protection

> [!WARNING]
> **Correction from v1**: Application-level check alone has a race condition — two async events could both read "no existing suggestion" before either writes. We use both layers.

**Layer 1 — Application check** (fast path, avoids unnecessary work):
```java
// In the async handler, before creating a suggestion:
boolean alreadyExists = suggestionRepository
    .existsByOrderIdAndTriggerReasonAndStatus(orderId, TriggerReason.AGENT_OFFLINE, SuggestionStatus.PENDING);
if (alreadyExists) {
    log.info("Skipping duplicate re-plan suggestion for order {}", orderId);
    continue;
}
```

**Layer 2 — Database unique constraint** (catches the race condition):
```sql
ALTER TABLE reassignment_suggestions
  ADD CONSTRAINT uq_pending_replan_per_order
  UNIQUE (order_id, trigger_reason, status);
```

> **Note**: This constraint only needs to be unique for `PENDING` rows. In MySQL, we can use a partial-index workaround or accept the broader constraint (only one suggestion per order/trigger/status combination). For the hackathon, the broader constraint is acceptable since we don't create multiple PENDING suggestions for the same order+trigger anyway.

```java
// In the async handler, after the app-level check:
try {
    suggestionRepository.save(suggestion);
} catch (DataIntegrityViolationException e) {
    log.info("Concurrent duplicate caught by DB constraint for order {}", orderId);
    // Safe to skip — another thread already created the suggestion
}
```

> **Scoring link**: Agentic loop — trigger (7 pts): event-driven, non-blocking, idempotent with dual-layer protection. Agentic loop — checkpoint (8 pts): suggestions queued, never auto-assigned.

### 5.4 Sprint 3 Extensibility — SLA Breach Trigger

The event mechanism is general. Sprint 3 adds an `SlaBreachEvent`:

```java
public record SlaBreachEvent(String orderId, Instant deadline) {}
```

A scheduled monitor publishes this event. The same `@EventListener` pattern handles it, calling the same routing interface. The extension seam is: **new event type → new listener → same `RoutingStrategy` interface**.

---

## 6. React UI — Ops Interface (Floor)

### 6.1 Tech Choice

- **React 18** with Vite (`npm create vite@latest`)
- Vanilla CSS (no Tailwind unless time permits)
- `fetch()` for API calls — no Axios needed for 4 endpoints

### 6.2 Component Tree

```
<App>
  ├── <Header>              — "ZipRun Ops Dashboard"
  ├── <AgentList>            — Shows all agents with status badges (colored)
  │     └── <AgentCard>      — Name, status, load
  ├── <ReassignmentQueue>    — Pending suggestions
  │     └── <SuggestionCard> — Order info, recommended agent, confidence bar,
  │           │                 AI reasoning (verbatim), Accept/Reject buttons,
  │           │                 RE-PLAN badge if triggerReason=AGENT_OFFLINE
  │           └── <ConfidenceBadge>
  └── <StatusBar>            — Last refresh time, strategy in use, error toast
```

### 6.3 Polling

- Poll `GET /api/suggestions?status=PENDING` every 5 seconds
- Also poll `GET /api/agents` every 5 seconds
- `useEffect` with `setInterval`, cleanup on unmount
- Manual "Refresh Now" button as well

### 6.4 Floor Checklist (12 pts)

| Requirement | Component | Status |
|---|---|---|
| Orders in REASSIGNMENT_PENDING with inline suggestion | `<SuggestionCard>` | ✅ |
| Accept/Reject buttons wired to PATCH | `<SuggestionCard>` buttons | ✅ |
| Re-plan badge (`triggerReason=AGENT_OFFLINE`) | `<SuggestionCard>` badge | ✅ |
| Agent list with status (AVAILABLE/BUSY/OFFLINE) | `<AgentList>` | ✅ |
| Polling or manual refresh | `setInterval` + button | ✅ |
| Loading states | Spinner in each section | ✅ |
| Error handling | Toast/banner on API error | ✅ |

> **Scoring link**: Ops interface floor (12 pts). Every checklist item is present.

---

## 7. Testing Strategy

Given the 5-hour constraint, testing is scoped surgically:

| Layer | What to Test | Tool | Time |
|---|---|---|---|
| **Routing contract** | Rule-based returns lowest-load agent | JUnit 5 unit test | 10 min |
| **AI fallback** | Verify rule-based fires when AI throws | JUnit 5 unit test with mocked LLMGateway | 10 min |
| **Idempotency** | Duplicate OFFLINE event doesn't create duplicate suggestions | Integration test or manual demo | 5 min |
| **API smoke test** | All 5 endpoints return correct status codes | Manual curl / Postman during development | Ongoing |
| **End-to-end demo** | Create order → PATCH agent OFFLINE → suggestion appears in UI → accept | Video demo | 5 min |

> **Scoring link**: Tests aren't explicitly scored but demonstrate correctness during the walkthrough (10 pts). The idempotency test is specifically called out as something to "show in your repository query."

---

## 8. ADR Decisions (5 Entries)

### ADR-1: Routing Logic Lives in a Dedicated `RoutingEngine` Service

- **Pattern**: Application Service (thin orchestrator) + Strategy beans
- **Why not controller?** Controllers shouldn't hold business logic.
- **Why not domain object?** `Order` shouldn't know about routing; it's a cross-cutting concern.
- **Why dedicated class?** Separates routing orchestration (strategy selection, context construction) from persistence, events, and HTTP concerns.

### ADR-2: Runtime Strategy Switching via Auto-Wired Bean Map + REST API

- **Options**: `@Qualifier` (restart required), `@Value` with env var (restart required for change), `Map<String, RoutingStrategy>` + `AtomicReference` + REST endpoint (true runtime)
- **Decision**: Bean map keyed by strategy name + `AtomicReference<String>` for active strategy name + `GET/PATCH /api/routing/strategy` endpoints. The `@Value` sets the default at startup; the REST endpoint mutates the `AtomicReference` for true runtime switching without restart.
- **Sprint 2 impact**: Adding `ZoneAffinityStrategy` = 1 new file, 0 existing files changed.
- **Tradeoff**: Strategy state is in-memory; a restart reverts to the config default. Acceptable for the hackathon. Production would persist the selection.

### ADR-3: LLM Unavailability — Cascading Fallback

- **Decision**: AI strategy catches all exceptions internally and delegates to rule-based strategy. The caller never sees an AI failure — they get a suggestion either way.
- **Tradeoff**: Ops doesn't see AI-quality reasoning when AI is down. Mitigated by `strategyUsed` field in the suggestion, so ops knows the recommendation came from the fallback.

### ADR-4: Agentic Loop via `ApplicationEventPublisher` + `@Async @EventListener`

- **Options**: `@Async` event listener, scheduled poller, message queue (Kafka/RabbitMQ)
- **Decision**: Spring domain events — zero infrastructure, event-driven (fires on state change, not timer), `@Async` keeps it off the request path.
- **Tradeoff**: In-process only; if the app crashes mid-replan, those suggestions are lost. Acceptable for a single-node hackathon. Sprint 2+ would use a persistent event store or message queue.

### ADR-5: Extensibility & Deliberate Exclusions

- **Extension seam**: `RoutingStrategy` interface + bean map = Sprint 2's `ZoneAffinityStrategy` is one new file. Nullable zone/capacity fields are already in the entity.
- **Deliberate exclusion**: Full dispatch board (UI ceiling) deferred because the agentic loop (T-4) is a correctness requirement and the dispatch board is a visibility enhancement. The 15 points in T-4 + the 9 points in AI resilience outweigh the +8 optional UI ceiling.

---

## 9. Project Structure

```
ai-reassignment-engine/
├── backend/
│   ├── src/main/java/com/ziprun/reassignment/
│   │   ├── ZipRunApplication.java
│   │   ├── config/
│   │   │   ├── AsyncConfig.java          — @EnableAsync, thread pool
│   │   │   └── CorsConfig.java           — CORS for localhost:5173
│   │   ├── model/
│   │   │   ├── Order.java                — @Entity
│   │   │   ├── Agent.java                — @Entity
│   │   │   ├── ReassignmentSuggestion.java — @Entity
│   │   │   ├── OrderStatus.java          — Enum
│   │   │   ├── AgentStatus.java          — Enum
│   │   │   ├── SuggestionStatus.java     — Enum
│   │   │   └── TriggerReason.java        — Enum
│   │   ├── repository/
│   │   │   ├── OrderRepository.java
│   │   │   ├── AgentRepository.java
│   │   │   └── SuggestionRepository.java
│   │   ├── routing/
│   │   │   ├── RoutingStrategy.java       — Interface
│   │   │   ├── RoutingRecommendation.java — Record
│   │   │   ├── TriggerContext.java        — Record
│   │   │   ├── RoutingEngine.java         — Strategy selector
│   │   │   ├── RuleBasedStrategy.java     — @Component("rule-based")
│   │   │   └── AiRoutingStrategy.java     — @Component("ai")
│   │   ├── ai/
│   │   │   ├── LLMGateway.java            — Addendum B
│   │   │   └── PromptBuilder.java         — Two prompts
│   │   ├── event/
│   │   │   ├── AgentOfflineEvent.java     — Domain event record
│   │   │   └── ReplanEventListener.java   — @Async @EventListener
│   │   ├── service/
│   │   │   ├── OrderService.java
│   │   │   ├── AgentService.java          — Publishes AgentOfflineEvent
│   │   │   └── SuggestionService.java
│   │   ├── controller/
│   │   │   ├── OrderController.java
│   │   │   ├── AgentController.java
│   │   │   ├── SuggestionController.java
│   │   └── RoutingController.java     — GET/PATCH strategy switching
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java — @ControllerAdvice
│   │       └── ResourceNotFoundException.java
│   ├── src/main/resources/
│   │   ├── application.properties         — MySQL config, LLM config
│   │   └── data.sql                       — Addendum A seed data
│   └── pom.xml                            — mysql-connector-j dependency
├── frontend/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── components/
│   │   │   ├── AgentList.jsx
│   │   │   ├── SuggestionQueue.jsx
│   │   │   └── SuggestionCard.jsx
│   │   ├── api/
│   │   │   └── client.js                  — fetch wrappers
│   │   └── index.css
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
├── ADR.md
└── README.md
```

---

## 10. Time Budget (5 Hours)

| Phase | Duration | Deliverables | Scoring Impact |
|---|---|---|---|
| **Phase 1**: Read, sketch, ADR-1 | 15 min | Architecture decisions locked, ADR-1 written | ADR (10 pts) |
| **Phase 2**: Domain model + API + routing engine | 70 min | 3 entities, 6 endpoints, `RoutingStrategy` interface, rule-based impl, bean map switching, ADR-2 | 20 + 25 pts |
| **Phase 3**: AI strategy + both prompts | 60 min | `AiRoutingStrategy`, `PromptBuilder`, all fallback paths, ADR-3 | 25 pts |
| **Phase 4**: Agentic re-plan loop | 50 min | Event publisher, async listener, idempotency guard, ADR-4 | 15 pts |
| **Phase 5**: React ops interface | 45 min | Floor UI — suggestion list, accept/reject, re-plan badge, agent list, polling | 12 pts |
| **Phase 6**: ADR-5, README, demo video | 35 min | ADR-5 (extensibility + exclusions), README, 5-min video | 10 pts walkthrough |
| **Total** | **~275 min (4h 35m)** | **25 min buffer** | **117 pts possible** |

> **Drop order if behind**: SSE bonus (+5) → UI ceiling (+8) → polish ADR-5 → never drop the agentic loop or AI fallback.

---

## 11. Risk Mitigation

| Risk | Mitigation |
|---|---|
| LLM API key issues / rate limits | Rule-based strategy works with zero external deps — demo still shows full flow |
| Spring Boot setup overhead | Use Spring Initializr with Web, JPA, MySQL Driver, Validation, Actuator pre-selected |
| React setup overhead | `npm create vite@latest . -- --template react` — under 2 minutes |
| CORS issues | Explicit `WebMvcConfigurer` CORS config from minute one |
| N+1 queries in routing | Eager-fetch agent list once, pass to strategy — no lazy-load surprises |
| Async event silent failure | Wrap entire `@EventListener` in try-catch, log all errors, fallback to rule-based |

---

## 12. Scoring Matrix — Design Decision → Points

| Design Decision | Directly Scores | Points |
|---|---|---|
| Clean state machines + nullable sprint 2 fields | Entity design | 8 |
| Correct HTTP verbs, structured error response | API correctness | 7 |
| Unidirectional JPA, no N+1, MySQL with data.sql | Persistence | 5 |
| `RoutingStrategy` interface, works from HTTP + async | Routing contract | 10 |
| Bean map + config property, no restart | Runtime switchability | 8 |
| ADR-2 with alternatives and sprint 2 extensibility | Pattern justification | 7 |
| Structured initial prompt with roster + output schema | Initial prompt quality | 8 |
| Recovery-framed re-plan prompt with offline context | Re-plan prompt quality | 8 |
| Fallback for every failure mode, ID validation | AI resilience | 9 |
| `@Async @EventListener`, event-driven on OFFLINE | Agentic loop trigger | 7 |
| Queue suggestions, human approves, never auto-assign | Agentic loop checkpoint | 8 |
| Full floor checklist: list, badge, buttons, poll, loading | Ops interface floor | 12 |
| 4+ entries, context/options/decision/tradeoffs format | ADR quality | 10 |
| Can trace order path + OFFLINE re-plan in code | Live walkthrough | 10 |
| **Total** | | **117** |

---

## 13. Key Architectural Principles

1. **Strategy Pattern over conditionals** — New routing algorithms are new classes, not new `if` branches.
2. **Event-driven over polling** — The agentic loop fires because something changed, not because a timer ticked.
3. **Fail-open with degradation** — AI failure produces a rule-based suggestion, never silence.
4. **Human-in-the-loop** — Suggestions are queued, not executed. Ops makes the final call.
5. **Extension seams over YAGNI** — Nullable fields and generic event types cost nothing now and save migrations later.
6. **Thin controllers, rich services** — Controllers validate and delegate. Services orchestrate. Strategies compute.

---

## 14. Database: MySQL Configuration

> [!WARNING]
> **Correction from v1**: The original proposal mentioned H2 in several places. The actual environment has **MySQL Workbench** installed. All references standardized to MySQL.

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/ziprun_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${MYSQL_PASSWORD:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=true
spring.sql.init.mode=always
spring.sql.init.data-locations=classpath:data.sql
```

**Maven dependency**:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Seed data adaptation**: The `data.sql` from Addendum A is H2/Postgres compatible. For MySQL, replace `NOW()` with `NOW()` (same syntax) and ensure `INSERT` uses `INSERT IGNORE` or the script runs only on empty tables to avoid duplicate key errors on restart.

---

## 15. Corrections Log (v1 → v2)

| # | Issue | v1 (Wrong) | v2 (Corrected) |
|---|---|---|---|
| 1 | Runtime switching | `@Value` reads once at startup; env var change needs restart | `AtomicReference` + `PATCH /api/routing/strategy` endpoint for true runtime switching |
| 2 | Database | Mixed H2 references | Standardized to **MySQL** throughout |
| 3 | Async enforcement | Mentioned but not emphasized | Added explicit `[!CAUTION]` — `@EnableAsync` + `@Async` on listener is mandatory, else PATCH blocks |
| 4 | Idempotency | App-level check only (race condition) | Dual-layer: app-level check + DB unique constraint |
| 5 | Accept suggestion | Only updates `Suggestion.status` | Full chain: Suggestion→ACCEPTED, Order→REASSIGNED, Agent load updated, all in `@Transactional` |

---

> [!IMPORTANT]
> **Next step**: Architecture is reviewed and corrected. Ready to implement **Phase 1: Domain Model + API** first. Want me to proceed?
