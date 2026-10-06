# Architecture Decision Records (ADR)

## Project: ZipRun AI Reassignment Engine
**Author:** Rakesh  
**Date:** October 2026  
**Status:** Accepted & Implemented  

---

### ADR-1: Dedicated Routing Engine Orchestration Component

* **Status:** Accepted
* **Context:**  
  When an order requires initial routing or emergency reassignment, candidate filtering, strategy resolution, prompt building, validation, and fallback handling must take place. We evaluated placing this logic directly inside `OrderController`, inside the `Order` entity, or in a dedicated `RoutingEngine` component.
* **Decision:**  
  Created a dedicated `RoutingEngine` service that orchestrates routing decisions across pluggable strategies. `RoutingEngine` maintains an `AtomicReference<String>` of the active strategy name and delegates execution to the appropriate implementation of the `RoutingStrategy` interface.
* **Rationale:**  
  - Controllers should only handle HTTP protocol concerns, serialization, and status codes.
  - Entities like `Order` must remain pure domain models without dependencies on external LLM clients or algorithmic routing logic.
  - A dedicated engine separates routing orchestration from persistence and domain event publishing.
* **Tradeoffs & Mitigations:**  
  Adds one extra layer of indirection, but completely isolates routing algorithms from HTTP controllers and database persistence.

---

### ADR-2: Runtime Strategy Switching via Strategy Pattern

* **Status:** Accepted
* **Context:**  
  Operations requires the ability to switch between `rule-based` (deterministic load-balancing) and `ai` (LLM reasoning via Zycus LiteLLM) strategies dynamically without taking the service down or restarting the JVM.
* **Decision:**  
  Implemented the Gang-of-Four **Strategy Pattern**:
  - `RoutingStrategy` interface defining `recommend(Order, List<Agent>, TriggerContext)`.
  - Concrete implementations: `RuleBasedStrategy` (`"rule-based"`) and `AiRoutingStrategy` (`"ai"`).
  - Spring auto-wires all beans into `Map<String, RoutingStrategy>` in `RoutingEngine`.
  - Exposed `GET /api/routing/strategy` and `PATCH /api/routing/strategy` endpoints to toggle strategies live.
* **Rationale:**  
  - Zero-downtime operations during high-traffic surges or external LLM maintenance.
  - Clean Open/Closed Principle: Adding a future `ZoneAffinityStrategy` in Sprint 2 requires adding exactly one new class implementing `RoutingStrategy` without modifying existing code.
* **Tradeoffs & Mitigations:**  
  Runtime lookup requires defensive validation to ensure unrecognized strategy names throw clean 400 Bad Request responses instead of runtime null pointers.

---

### ADR-3: LLM Resilience and Cascading Fallback Matrix

* **Status:** Accepted
* **Context:**  
  External LLMs can fail due to network timeouts, HTTP 429 rate limits, malformed JSON responses, or model hallucinations (e.g., returning an agent ID that does not exist or is offline). An automated logistics engine must never leave a stranded delivery hanging without a recommendation.
* **Decision:**  
  Implemented a resilient cascading fallback inside `AiRoutingStrategy`:
  1. **Strict 10s Read/Connect Timeout** configured in `RestClient`.
  2. **Hallucination Defense**: Validates the LLM-recommended `agentId` against the actual list of candidate available agents.
  3. **Robust Markdown JSON Extractor**: Strips markdown code fences (````json ... ````) and extracts outermost JSON braces.
  4. **Fallback Delegation**: Any caught exception triggers an immediate fallback to `RuleBasedStrategy.recommend(...)` with a logged `WARN`.
  5. **Audit Transparency**: The suggestion entity records `strategyUsed: "ai"` alongside the generated reasoning so Ops knows exactly how the recommendation was derived.
* **Rationale:**  
  Guarantees 100% operational continuity. A delivery suggestion is always generated for Ops approval regardless of external AI endpoint availability.
* **Tradeoffs & Mitigations:**  
  When falling back, Ops receives rule-based load-balancing reasoning rather than deep contextual reasoning. This is mitigated by explicitly logging the failure and providing a transparent confidence score.

---

### ADR-4: Asynchronous Agentic Loop via Spring Domain Events

* **Status:** Accepted
* **Context:**  
  When an agent transitions to `OFFLINE`, their assigned orders become stranded. If the reassignment loop runs synchronously inside the `PATCH /api/agents/{id}/status` request, the web request would block for 2–5 seconds while calling the LLM, leading to poor user experience, frontend timeouts, and thread pool exhaustion under concurrency.
* **Decision:**  
  Adopted an event-driven architecture using Spring's domain event model:
  - `AgentService` publishes an immutable `AgentOfflineEvent(agentId, agentName)` record.
  - The HTTP request immediately returns `HTTP 200 OK` (latency < 15ms).
  - `ReplanEventListener` catches the event asynchronously via `@Async @EventListener` on a dedicated thread pool (`replan-*`) configured in `AsyncConfig` (4 core threads, 8 max threads, 50 queue capacity).
  - Implemented a two-layer idempotency guard:
    - Layer 1: Application-level check (`suggestionRepository.existsByOrderIdAndTriggerReasonAndStatus(...)`).
    - Layer 2: Database unique index (`uq_pending_replan_per_order`).
* **Rationale:**  
  - Decouples user-facing latency from heavy background computations.
  - Zero external infrastructure overhead (no Kafka/RabbitMQ required for single-node hackathon requirements).
* **Tradeoffs & Mitigations:**  
  In-memory events do not survive an unexpected server hard crash mid-flight. For production multi-node scale, this would be backed by transactional outbox or Kafka, but for the hackathon single-node deployment, Spring domain events provide optimal performance with zero operational complexity.

---

### ADR-5: Extensibility and Deliberate Exclusions

* **Status:** Accepted
* **Context:**  
  The hackathon brief emphasizes delivering a robust reactive reassignment engine with clear extension seams for Sprints 2 & 3, while making conscious priority trade-offs.
* **Deliberate Inclusions & Extension Seams:**  
  1. **Sprint 2 Placeholders**: Pre-populated nullable fields in entities (`Order.pickupZone`, `Order.dropoffZone`, `Order.weightClass`, `Agent.currentZone`, `Agent.maxCapacity`).
  2. **Strategy Seam**: `RoutingStrategy` interface is ready for `ZoneAffinityStrategy` without touching `RuleBasedStrategy` or `AiRoutingStrategy`.
  3. **Human-in-the-Loop Checkpoint**: Preserved mandatory human approval (`PENDING` -> `ACCEPTED`/`REJECTED`) to ensure operations maintains final accountability over deliveries.
* **Deliberate Exclusions & Priority Trade-offs:**  
  1. **Full Dispatch Board**: Deferred building a live GPS-map and order-creation dashboard. We prioritized the core agentic loop (T-4), LLM resilience, and idempotency correctness over optional visual ceilings.
  2. **Server-Sent Events (SSE) / WebSockets**: Used periodic polling on the dashboard. Polling requires zero stateful socket management, zero firewall friction, and provides reliable data synchronization.
* **Rationale:**  
  Delivering rock-solid correctness on core scoring criteria (117 pts) takes priority over half-implemented optional extensions.
