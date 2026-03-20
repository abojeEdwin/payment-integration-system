# Phase 2 Development Workflow Guide

## Overview

Your services (auth, payment, webhook) can now run **from the IDE with full Postgres + Kafka support** via Docker containers on your host machine.

---

## Architecture: IDE + Docker Hybrid

```
┌─────────────────────────────────────────────────────────────┐
│ Your Machine (Host)                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  IDE                          Docker Containers            │
│  ─────────────────            ─────────────────            │
│  auth-service      ─────────→ postgres:5432 (exposed as)   │
│  (run config: dev)            localhost:5432               │
│                               ↑                             │
│  payment-service ──────────→  └→ kafka:29092 (exposed as)  │
│  (run config: dev)                localhost:9092           │
│                               ↑                             │
│  webhook-service ──────────→  pgadmin:8080                 │
│  (run config: dev)            kafka-ui:8090                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Key insight:** Docker containers expose their ports to your host (`localhost:PORT`), so IDE services connect the same way!

---

## Setup: First Time Only

```bash
cd /home/civm/Documents/phoenix/payment-integration-system

# Create .env from template
cp .env.example .env

# Verify postgres is on 5432 in docker
docker compose -f compose.yaml ps | grep postgres
# Output should show: postgres ... 5432->5432

# If 5432 is busy on your machine, set in .env:
echo "POSTGRES_HOST_PORT=5433" >> .env
```

---

## Daily Workflow

### **Option A: IDE + Docker Infra (RECOMMENDED for dev speed)**

Start only infrastructure containers (fast startup):

```bash
cd /home/civm/Documents/phoenix/payment-integration-system

# Start postgres, kafka, UIs (not app services)
docker compose -f compose.yaml up -d postgres kafka kafka-ui pgadmin
docker compose -f compose.yaml ps
```

Then run **one service from IntelliJ IDE**:

1. Open **Run → Edit Configurations**
2. Select your service (e.g., `auth-service`)
3. Set **Active profiles** to `dev`
4. Click **Run**

The IDE service will:
- Load `.env` environment variables
- Connect to `localhost:5432` → reaches Docker postgres
- Connect to `localhost:9092` → reaches Docker kafka
- Listen on `localhost:8081` (auth) / `8082` (payment) / `8083` (webhook)

**Benefit:** Fastest dev loop, instant restarts, debugger works perfectly.

---

### **Option B: Full Docker Stack (integration testing)**

Run all services in Docker (mimics production):

```bash
cd /home/civm/Documents/phoenix/payment-integration-system

# Rebuild all service JARs
./mvnw -q -DskipTests -pl auth-service,payment-service,webhook-service -am package

# Start entire stack
docker compose -f compose.yaml up -d --build

# Watch logs
docker compose -f compose.yaml logs -f --tail=200
```

Services will:
- Use container DNS names (`postgres:5432`, `kafka:29092`)
- Docker Compose automatically injects these env vars (no .env needed)
- See each other via internal Docker network
- All accessible from host via `localhost:PORT`

**Benefit:** Tests real cross-service networking, Kafka delivery, container isolation.

---

## Verifying Connection Strings

### Check what your running service sees:

```bash
# If running auth-service from IDE, check its actual config:
curl -s http://localhost:8081/actuator/configprops 2>/dev/null | grep -A5 "datasource.url"

# Or check IDE console logs for this line:
# "HikariPool-1 - Start completed."
```

### Connection string mapping:

| Service | Mode | Env Var | Value | Connects To |
|---------|------|---------|-------|-------------|
| auth-service | IDE | `AUTH_DB_URL` | `jdbc:postgresql://localhost:5432/auth_db` | Docker postgres |
| auth-service | Docker | (from compose.yaml) | `jdbc:postgresql://postgres:5432/auth_db` | Container DNS |
| payment-service | IDE | `PAYMENT_DB_URL` | `jdbc:postgresql://localhost:5432/payment_db` | Docker postgres |
| payment-service | Docker | (from compose.yaml) | `jdbc:postgresql://postgres:5432/payment_db` | Container DNS |

---

## Common Issues & Fixes

### Issue: "Cannot connect to postgres" when running from IDE

**Cause:** Postgres container not running or port mapped incorrectly.

**Fix:**
```bash
# Check container status
docker compose -f compose.yaml ps postgres

# If not healthy, restart
docker compose -f compose.yaml restart postgres

# Check port mapping
docker compose -f compose.yaml ps | grep postgres
# Should show: 5432->5432 (or your POSTGRES_HOST_PORT->5432)
```

### Issue: "Port 5432 already in use"

**Cause:** You have a local postgres service running.

**Fix:**
```bash
# Option 1: Stop local postgres
# (system-dependent, e.g., brew services stop postgresql@15)

# Option 2: Use different host port in .env
echo "POSTGRES_HOST_PORT=5433" >> .env

# Then update .env database URLs:
# AUTH_DB_URL=jdbc:postgresql://localhost:5433/auth_db
# PAYMENT_DB_URL=jdbc:postgresql://localhost:5433/payment_db
# WEBHOOK_DB_URL=jdbc:postgresql://localhost:5433/webhook_db
```

### Issue: Service in Docker can't reach another service

**Cause:** Using `localhost` instead of container DNS.

**Fix:** Docker Compose sets env vars automatically. Verify compose.yaml has the right URLs (they should use `postgres:5432`, not `localhost`).

---

## Before Every Commit

```bash
cd /home/civm/Documents/phoenix/payment-integration-system

# 1. Rebuild with no skips
./mvnw -q -DskipTests -pl auth-service,payment-service,webhook-service -am package

# 2. Run unit tests
./mvnw test

# 3. Validate compose syntax
docker compose -f compose.yaml config

# 4. Start full stack
docker compose -f compose.yaml up -d --build

# 5. Smoke tests (wait ~10s for startup)
sleep 10

# Register + Login test
email="test_$(date +%s)@example.com"
curl -X POST http://localhost:8081/auth/register \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Test\",\"email\":\"$email\",\"password\":\"Pass123!\",\"provider\":\"PAYSTACK\"}"

curl -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$email\",\"password\":\"Pass123!\"}"

# 6. Stop stack
docker compose -f compose.yaml down
```

---

## Environment Variables Reference

All variables in `.env`:

- **Postgres:** `POSTGRES_HOST_PORT`, `POSTGRES_SUPERUSER_PASSWORD`, `APP_DB_PASSWORD`
- **Auth:** `AUTH_DB_URL`, `AUTH_DB_USERNAME`, `AUTH_DB_PASSWORD`, `JWT_SECRET`
- **Payment:** `PAYMENT_DB_URL`, `PAYMENT_DB_USERNAME`, `PAYMENT_DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`
- **Webhook:** `WEBHOOK_DB_URL`, `WEBHOOK_DB_USERNAME`, `WEBHOOK_DB_PASSWORD`
- **Service URLs (IDE):** `AUTH_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `WEBHOOK_SERVICE_URL`
- **Providers:** `PAYSTACK_SECRET_KEY`, `INTERSWITCH_SECRET_KEY`, `SQUADCO_SECRET_KEY`

---

## Troubleshooting Checklist

- [ ] `.env` file exists and has `AUTH_DB_URL=jdbc:postgresql://localhost:5432/auth_db`
- [ ] Docker daemon is running: `docker ps`
- [ ] Postgres container is healthy: `docker compose -f compose.yaml ps postgres`
- [ ] You selected profile `dev` in IntelliJ Run Configuration
- [ ] Firewall isn't blocking localhost ports (rare on dev machines)

---

## Next: Advanced Topics

- **Migrations:** Set up Flyway for schema versioning
- **CI/CD:** GitHub Actions to validate compose on PR
- **Load testing:** Use Kafka UI + pgAdmin to observe under load
- **Debugging:** Set breakpoints in IDE, containers won't interfere

---

Done! You now have a **solid phase-2 dev environment**. 🚀

