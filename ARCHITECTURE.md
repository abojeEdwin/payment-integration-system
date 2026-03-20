# Architecture Diagram: Phase 2 Development Setup

## Local Development: IDE + Docker Hybrid

```
┌─────────────────────────────────────────────────────────────────────────┐
│  YOUR LAPTOP                                                            │
│                                                                         │
│  ┌─────────────────────────────────────┐  ┌─────────────────────────┐  │
│  │  IntelliJ IDE                       │  │  Docker Containers      │  │
│  │  (Running on Host Machine)          │  │  (paybridge-dev stack)  │  │
│  ├─────────────────────────────────────┤  ├─────────────────────────┤  │
│  │                                     │  │                         │  │
│  │  ┌─ auth-service                  │  │  ┌─ postgres:5432      │  │
│  │  │  :8081 (dev profile)            │─────→│  exposed as:         │  │
│  │  └─ connects via .env              │  │  │  localhost:5432      │  │
│  │     AUTH_DB_URL=                   │  │  │                      │  │
│  │     localhost:5432/auth_db         │  │  │  Databases:          │  │
│  │                                     │  │  │  - auth_db           │  │
│  │  ┌─ payment-service                │  │  │  - payment_db        │  │
│  │  │  :8082 (dev profile)            │  │  │  - webhook_db        │  │
│  │  │  connects via .env              │  │  │                      │  │
│  │  └─ PAYMENT_DB_URL=                │  │  └──────────────────────┘  │
│  │     localhost:5432/payment_db      │  │                            │
│  │                                     │  │  ┌─ kafka:9092           │  │
│  │  ┌─ webhook-service                │  │  │  exposed as:          │  │
│  │  │  :8083 (dev profile)            │─────→│  localhost:9092       │  │
│  │  │  connects via .env              │  │  │                       │  │
│  │  └─ WEBHOOK_DB_URL=                │  │  │  Dual-listener:       │  │
│  │     localhost:5432/webhook_db      │  │  │  - 29092 (internal)   │  │
│  │                                     │  │  │  - 9092 (host)        │  │
│  │                                     │  │  └────────────────────── │  │
│  │  Debugger:          ✓ Works!       │  │                            │
│  │  Live Reload:       ✓ Works!       │  │  ┌─ pgadmin              │  │
│  │  Breakpoints:       ✓ Works!       │  │  │  :8080 (DB UI)         │  │
│  │  Instant Restarts:  ✓ Works!       │  │  └──────────────────────┘  │
│  │                                     │  │                            │
│  └─────────────────────────────────────┘  │  ┌─ kafka-ui             │  │
│                                            │  │  :8090 (Kafka UI)      │  │
│  .env file (local secrets):                │  └──────────────────────┘  │
│  ✓ AUTH_DB_URL=localhost:5432/auth_db     │                            │
│  ✓ PAYMENT_DB_URL=localhost:5432/payment  │                            │
│  ✓ WEBHOOK_DB_URL=localhost:5432/webhook  │                            │
│  ✓ KAFKA_BOOTSTRAP_SERVERS=localhost:9092 │                            │
│  ✓ Service URLs all set to localhost      │                            │
│                                            │                            │
└─────────────────────────────────────────────────────────────────────────┘

                              ↕ Switch Modes ↕

┌─────────────────────────────────────────────────────────────────────────┐
│  FULL DOCKER MODE (Integration Testing)                                 │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Docker Compose Network: paybridge-network                      │  │
│  │                                                                 │  │
│  │  ┌─ auth-service (container)      ┌─ postgres (container)    │  │
│  │  │  :8081 (in container)           │  :5432 (in container)    │  │
│  │  │  uses env var injected by       │  host port: localhost:5432
│  │  │  compose.yaml                   └─ auth_user role         │  │
│  │  │  auth-service → postgres:5432   ┌─ kafka (container)      │  │
│  │  └─────────────────┬─────────────→ │  :29092 (internal) host:
│  │                    │               │  :9092 (localhost)       │  │
│  │  ┌─ payment-service│──────────────┼─ payment_user role      │  │
│  │  │  :8082          │               │                         │  │
│  │  │  → postgres:5432 ──────────────→└──────────────────────── │  │
│  │  │  → kafka:29092   │                                        │  │
│  │  └─────────────────┤────┐                                    │  │
│  │                    │    │                                    │  │
│  │  ┌─ webhook-service│────┼──────┐                             │  │
│  │  │  :8083          │    │      │                             │  │
│  │  │  → postgres:5432 ────┼──→ pgadmin :8080                 │  │
│  │  │  → kafka:29092        │                                  │  │
│  │  └──────────────────────┘      kafka-ui  :8090              │  │
│  │                                                               │  │
│  │  All services use CONTAINER DNS NAMES (postgres, kafka, etc)│  │
│  │  Docker Compose automatically injects these via env vars     │  │
│  │                                                               │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  Accessible from HOST via localhost:PORT (port mappings)               │
│  All services talk to each other via Docker internal DNS               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Connection String Magic

| Scenario | Service | Postgres Host | Port | Database | User |
|----------|---------|---------------|------|----------|------|
| IDE Dev | auth-service (IDE) | `localhost` | 5432 | `auth_db` | `auth_user` |
| IDE Dev | payment-service (IDE) | `localhost` | 5432 | `payment_db` | `payment_user` |
| IDE Dev | webhook-service (IDE) | `localhost` | 5432 | `webhook_db` | `webhook_user` |
| Docker | auth-service (container) | `postgres` | 5432 | `auth_db` | `auth_user` |
| Docker | payment-service (container) | `postgres` | 5432 | `payment_db` | `payment_user` |
| Docker | webhook-service (container) | `postgres` | 5432 | `webhook_db` | `webhook_user` |

**Key:** Service finds its database via **Docker DNS name** (`postgres`) when running in container, or **localhost** when running on host.

---

## Daily Workflow Decision Tree

```
┌─ START OF DAY
│
├─ Want SPEED (hot reload, breakpoints, fast restarts)?
│  └─→ Use IDE + Docker Hybrid Mode
│      $ docker compose -f compose.yaml up -d postgres kafka kafka-ui pgadmin
│      Run service from IntelliJ (dev profile)
│      Edit → Save → Automatic restart (1-2 sec)
│
├─ Want to TEST INTEGRATION (cross-service calls, Kafka delivery)?
│  └─→ Use Full Docker Mode
│      $ docker compose -f compose.yaml up -d --build
│      All services in containers
│      Wait ~30s for startup
│      Test via localhost:PORT
│
└─ Want to VERIFY BEFORE COMMIT?
   └─→ Run BOTH modes in sequence
       1. IDE test (fast dev cycle)
       2. Docker test (integration check)
       3. Then commit ✅
```

---

## Key Files

| File | Purpose | Commit? |
|------|---------|---------|
| `.env` | Your local secrets (Postgres password, API keys) | ❌ NO (.gitignore) |
| `.env.example` | Template for teammates | ✅ YES |
| `compose.yaml` | Docker Compose definition (Postgres, Kafka, etc) | ✅ YES |
| `DEVELOPMENT.md` | Step-by-step workflow guide | ✅ YES |
| `DEV_SETUP_COMPLETE.md` | Setup checklist (this confirms it works) | ✅ YES |
| `application-dev.yml` | Spring Boot dev profile config | ✅ YES |

---

## Quick Validation Checklist

- [ ] `.env` exists with `AUTH_DB_URL=jdbc:postgresql://localhost:5432/auth_db`
- [ ] `docker compose ps postgres` shows container **healthy**
- [ ] `docker compose ps postgres` shows port **5432->5432**
- [ ] You can open http://localhost:8080 (pgAdmin) in browser
- [ ] You can open http://localhost:8090 (Kafka UI) in browser
- [ ] Run auth-service from IDE → connects to Postgres ✅
- [ ] Create merchant via `POST /auth/register` → saves to Postgres ✅
- [ ] Login via `POST /auth/login` → JWT returned ✅

---

**You're ready for Phase 2 development!** 🎉

