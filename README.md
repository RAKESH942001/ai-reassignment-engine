# ZipRun AI Reassignment Engine

An event-driven, AI-powered reactive reassignment engine built for delivery fleet operations. When an active delivery agent goes offline mid-shift, ZipRun automatically detects stranded deliveries, evaluates candidate agents using Zycus LLM (`qwen-cursor`) with fallback to rule-based load-balancing, and presents actionable recommendations to operations managers on an interactive React dashboard with a human-in-the-loop approval checkpoint.

---

## Problem Statement

At ZipRun, orders are assigned to delivery agents at the start of each shift. When an agent abruptly becomes unavailable mid-day (bike breakdown, illness, app crash):
1. **Silent Stagnation:** Affected deliveries sit idle until a customer complains or ops notices.
2. **Cognitive Overload:** Ops managers spend 20–45 minutes opening spreadsheets, checking driver loads, and manually calculating routes.
3. **Dispatch Bottlenecks:** Human dispatch doesn't scale during peak rush hours.

---

## Solution

ZipRun replaces manual dispatch fire-fighting with an automated **Agentic Reassignment Engine**:
- **Automatic Event Trigger:** Transitioning an agent to `OFFLINE` publishes an internal domain event.
- **Non-Blocking Re-planning:** Web APIs respond in `< 15ms`, while background workers asynchronously process re-routing.
- **Dual Routing Strategies:** Runtime-switchable between deterministic `rule-based` load balancing and context-aware `ai` optimization using Zycus LiteLLM.
- **Resilient Fallback:** If the external LLM is unreachable or hallucinates, the system automatically degrades to rule-based recommendations without dropping orders.
- **Human-in-the-Loop:** Suggestions are queued on a React dashboard with AI reasoning and confidence metrics for 1-click Ops approval.

---

## System Architecture

```text
                     +---------------------------------------+
                     |         React 18 Ops Dashboard         |
                     |  (Agent Fleet, Suggestion Queue, Hub) |
                     +---------------------------------------+
                                         |
                                         | REST APIs (HTTP / JSON)
                                         v
                     +---------------------------------------+
                     |      Spring Boot 3.2.5 REST Layer     |
                     |  (/api/agents, /api/orders, /routing) |
                     +---------------------------------------+
                                         |
               +-------------------------+-------------------------+
               |                                                   |
               v                                                   v
   +-----------------------+                           +-----------------------+
   |     AgentService      |                           |     OrderService      |
   | (Updates Status & DB) |                           | (Manages Order State) |
   +-----------------------+                           +-----------------------+
               |
               | Publishes AgentOfflineEvent
               v
   +---------------------------------------------------------------+
   |             Asynchronous Event Listener                       |
   |   ReplanEventListener (@Async on ThreadPool 'replan-*')       |
   +---------------------------------------------------------------+
               |
               v
   +---------------------------------------------------------------+
   |                      RoutingEngine                            |
   |     Strategy Pattern: Map<String, RoutingStrategy>            |
   +---------------------------------------------------------------+
               |                                   |
               | [Active: ai]                      | [Active: rule-based]
               v                                   v
   +-----------------------+           +-----------------------+
   |   AiRoutingStrategy   |           |   RuleBasedStrategy   |
   | (Zycus LiteLLM Proxy) |           |  (Lowest Active Load) |
   +-----------------------+           +-----------------------+
               | (On Exception)                    ^
               +------------- Cascading Fallback -+
               |
               v
   +---------------------------------------------------------------+
   |               ReassignmentSuggestion (PENDING)                |
   |         (Persisted in PostgreSQL with DB Unique Index)        |
   +---------------------------------------------------------------+
                                         |
                                         v
                     +---------------------------------------+
                     |        Ops Approval / Rejection       |
                     |         (Atomic State Chain)          |
                     +---------------------------------------+
```

---

## Key Features

- **Runtime Strategy Switching:** Toggle between `ai` and `rule-based` routing live via `PATCH /api/routing/strategy` without restarting the JVM.
- **Two Distinct AI Prompts:** 
  - *Initial Prompt:* Optimal multi-agent matching based on load and location.
  - *Emergency Re-plan Prompt:* Recovery-framed prompt emphasizing workload redistribution away from the offline agent.
- **Cascading Fallback Matrix:** 10s HTTP timeouts, JSON fence stripping, hallucinated agent validation, and automatic rule-based fallback.
- **Two-Layer Idempotency Guard:** Application-level `existsBy...` query combined with a database unique constraint (`uq_pending_replan_per_order`).
- **Atomic State Transitions:** Accepting a suggestion atomically updates `Suggestion -> ACCEPTED`, `Order -> REASSIGNED`, and updates agent active order counts within a single `@Transactional` boundary.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.5, Spring Data JPA, Hibernate, Spring Validation, Actuator
- **Database:** PostgreSQL 17 (port `5433`, database `ziprun_db`) / H2 compatible
- **AI Gateway:** OpenAI-compatible REST Client targeting Zycus LiteLLM (`qwen-cursor`) with `product: PCT`
- **Frontend:** React 18, Vite, Vanilla Modern CSS (Tailored dark palette, responsive flex grid)
- **Testing:** JUnit 5, Mockito, AssertJ, Maven Surefire

---

## Project Structure

```text
ai-reassignment-engine/
├── backend/
│   ├── src/main/java/com/ziprun/reassignment/
│   │   ├── ai/              # OpenAiLlmGateway, PromptBuilder, LlmRoutingPayload
│   │   ├── config/          # AsyncConfig (replan thread pool), CorsConfig
│   │   ├── controller/      # AgentController, OrderController, SuggestionController, RoutingController
│   │   ├── dto/             # CreateOrderRequest, UpdateAgentStatusRequest, etc.
│   │   ├── event/           # AgentOfflineEvent, ReplanEventListener
│   │   ├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
│   │   ├── model/           # Agent, Order, ReassignmentSuggestion, Enums
│   │   ├── repository/      # AgentRepository, OrderRepository, SuggestionRepository
│   │   ├── routing/         # RoutingStrategy, RoutingEngine, RuleBasedStrategy, AiRoutingStrategy
│   │   └── service/         # AgentService, OrderService, SuggestionService
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── data.sql         # Seed data (5 agents, 8 orders)
│   ├── src/test/java/       # Comprehensive test suite (11 unit/integration tests)
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/      # Header, AgentList, SuggestionQueue, SuggestionCard, OrdersOverview
│   │   ├── api.js           # Centralized backend client
│   │   ├── App.jsx          # Root application container
│   │   └── index.css        # Premium Ops design system
│   ├── package.json
│   └── vite.config.js
│
├── ADR.md                   # 5 Architecture Decision Records
├── README.md                # Evaluator & setup documentation
├── .gitignore               # Strict secret & build exclusion rules
└── .env.example             # Environment variable template
```

---

## How to Run (< 5 Minutes Setup)

### Prerequisites
- Java 17+ installed (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- Node.js 18+ installed (`node -version`)
- PostgreSQL 17 running on port `5433` (or adjust `application.properties`)

### 1. Database Setup
Create database `ziprun_db` in PostgreSQL:
```sql
CREATE DATABASE ziprun_db;
```

### 2. Backend Startup
Set your environment variables and start Spring Boot:

**In PowerShell (Windows):**
```powershell
$env:LLM_BASE_URL="https://litellm-qc.zycus.net/v1"
$env:LLM_API_KEY="<your-api-key>"
$env:LLM_COOKIE="<your-cookie>"
$env:LLM_PRODUCT="PCT"
$env:LLM_MODEL="qwen-cursor"

cd backend
mvn spring-boot:run
```

**In Bash (Linux/macOS):**
```bash
export LLM_BASE_URL="https://litellm-qc.zycus.net/v1"
export LLM_API_KEY="<your-api-key>"
export LLM_COOKIE="<your-cookie>"
export LLM_PRODUCT="PCT"
export LLM_MODEL="qwen-cursor"

cd backend
mvn spring-boot:run
```
The backend initializes database tables, executes `data.sql`, and starts on **`http://localhost:8080`**.

### 3. Frontend Startup
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
Open your browser to: **`http://localhost:5173`**

---

## Configuration Reference

| Environment Variable | Default | Purpose |
| :--- | :--- | :--- |
| `LLM_BASE_URL` | *(empty)* | Endpoint for OpenAI-compatible chat completions |
| `LLM_API_KEY` | *(empty)* | Bearer authentication token (never logged) |
| `LLM_COOKIE` | *(empty)* | Cookie header for proxy authentication |
| `LLM_PRODUCT` | `PCT` | Product identification tag for Zycus LiteLLM |
| `LLM_MODEL` | `qwen-cursor` | LLM model identifier |
| `LLM_TIMEOUT_SECONDS`| `10` | Connect and read timeout |
| `SPRING_DATASOURCE_URL`| `jdbc:postgresql://localhost:5433/ziprun_db` | PostgreSQL connection URL |

---

## API Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/agents` | List all delivery agents and workloads | `200 OK` |
| `GET` | `/api/agents/{id}` | Get agent details by ID | `200 OK` |
| `PATCH` | `/api/agents/{id}/status` | Update agent status (`AVAILABLE`, `BUSY`, `OFFLINE`). Triggers async re-planning if set to `OFFLINE`. | `200 OK` |
| `GET` | `/api/orders` | List orders (optional `?status=ASSIGNED` filter) | `200 OK` |
| `POST` | `/api/orders` | Create new order assigned to an agent | `201 Created` |
| `POST` | `/api/orders/{id}/suggest` | Request on-demand reassignment suggestion | `201 Created` |
| `GET` | `/api/suggestions` | List suggestions (optional `?status=PENDING` filter) | `200 OK` |
| `PATCH` | `/api/suggestions/{id}` | Accept (`ACCEPTED`) or reject (`REJECTED`) a recommendation | `200 OK` |
| `GET` | `/api/routing/strategy` | Inspect active and available strategies | `200 OK` |
| `PATCH` | `/api/routing/strategy` | Switch active strategy (`ai` or `rule-based`) | `200 OK` |

---

## Agentic Re-planning Flow

```text
Agent goes OFFLINE
       │
       ▼
PATCH /api/agents/{id}/status ──► Returns HTTP 200 immediately (< 15ms)
       │
       ▼ (Internal Domain Event)
AgentOfflineEvent(agentId, agentName)
       │
       ▼ (Handled asynchronously on 'replan-*' thread pool)
ReplanEventListener
       │
       ├─► 1. Query stranded orders (Assigned to agent + status ASSIGNED)
       ├─► 2. Transition order status to REASSIGNMENT_PENDING
       ├─► 3. Filter candidate agents (Status in [AVAILABLE, BUSY], excluding offline agent)
       ├─► 4. Check idempotency (Application check + DB unique constraint)
       ├─► 5. Invoke RoutingEngine.recommend(...)
       │        ├── Calls Zycus LLM (AiRoutingStrategy)
       │        └── Falls back to RuleBasedStrategy on any error
       └─► 6. Save ReassignmentSuggestion with status PENDING
               │
               ▼
   Displayed on React Ops Dashboard for 1-Click Human Approval
```

---

## AI Fallback Matrix

| Failure Mode | Detection Mechanism | System Reaction |
| :--- | :--- | :--- |
| **Network Timeout (> 10s)** | `ResourceAccessException` in `RestClient` | Log `WARN`, fall back to `RuleBasedStrategy`, return suggestion |
| **HTTP 4xx / 5xx Error** | `RestClientResponseException` | Log `WARN`, fall back to `RuleBasedStrategy`, return suggestion |
| **Malformed / Invalid JSON** | `JsonProcessingException` | Log `WARN`, fall back to `RuleBasedStrategy`, return suggestion |
| **Hallucinated Agent ID** | Validation against active candidate set | Log `WARN` with hallucinated ID, fall back to `RuleBasedStrategy` |
| **Missing Credentials** | `IllegalStateException` on empty base URL | Log `WARN`, fall back to `RuleBasedStrategy` |

---

## Testing

Run the full backend test suite:
```bash
cd backend
mvn test
```

### Verified Test Suite (11/11 Passing):
- **`RuleBasedStrategyTest`**: Verifies candidate filtering, lowest workload selection, and alphabetical tie-breaking.
- **`PromptBuilderTest`**: Validates distinct structures for Initial Assignment vs. Emergency Recovery prompts.
- **`AiRoutingStrategyTest`**: 
  - Validates successful OpenAI-compatible JSON extraction.
  - Validates markdown code fence stripping (````json ... ````).
  - Validates hallucination defense when model returns an invalid agent ID.
  - Validates fallback to rule-based strategy when LLM fails or throws an exception.
- **`ZipRunApplicationTests`**: Verifies full Spring application context loading.

---

## Design Decisions (ADR Summary)

Full architectural rationales are documented in [`ADR.md`](ADR.md):
- **ADR-1:** Routing logic is isolated in a dedicated `RoutingEngine` component, keeping controllers and domain entities decoupled.
- **ADR-2:** Runtime strategy switching via the Strategy Pattern and Spring auto-wired bean map.
- **ADR-3:** Zero-drop LLM fallback matrix guaranteeing 100% operational continuity.
- **ADR-4:** Asynchronous agentic re-planning loop via Spring domain events on a dedicated worker pool.
- **ADR-5:** Extensible Sprint 2/3 domain seams and deliberate prioritization of core correctness over non-essential UI features.
