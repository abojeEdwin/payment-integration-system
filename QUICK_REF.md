# Quick Reference: IDE + Docker Development

## Right Now (Copy-Paste)

```bash
# Terminal 1: Start Docker infra only
cd /home/civm/Documents/phoenix/payment-integration-system
docker compose -f compose.yaml up -d postgres kafka kafka-ui pgadmin
docker compose -f compose.yaml ps
```

```bash
# Terminal 2: Run auth-service from IntelliJ
# → Open IntelliJ → Select auth-service
# → Edit Configurations → Active profiles: dev
# → Click Run
```

```bash
# Terminal 3: Test it
email="test_$(date +%s)@example.com"
curl -X POST http://localhost:8081/auth/register \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Test\",\"email\":\"$email\",\"password\":\"Pass123!\",\"provider\":\"PAYSTACK\"}" | jq .

curl -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$email\",\"password\":\"Pass123!\"}" | jq .token
```

---

## Connection Strings (in your `.env`)

| Service | DB | Host | Port | Database |
|---------|----|----|------|----------|
| auth-service | Postgres | localhost | 5432 | auth_db |
| payment-service | Postgres | localhost | 5432 | payment_db |
| webhook-service | Postgres | localhost | 5432 | webhook_db |
| all | Kafka | localhost | 9092 | — |

---

## Web UIs (Bookmark These)

- **PgAdmin:** http://localhost:8080 (admin@paybridge.dev / admin123)
- **Kafka UI:** http://localhost:8090

---

## IDE Service URLs

- Auth: http://localhost:8081
- Payment: http://localhost:8082
- Webhook: http://localhost:8083

---

## Before Every Commit

```bash
./mvnw -q -DskipTests -pl auth-service,payment-service,webhook-service -am package
docker compose -f compose.yaml up -d --build
sleep 10
# Test all flows...
docker compose -f compose.yaml down
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 5432 busy | Set `POSTGRES_HOST_PORT=5433` in `.env` + update URLs |
| Service won't connect | Check `docker compose ps postgres` is healthy |
| Kafka connection fails | Verify `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` in `.env` |
| IDE service can't find other services | Use `localhost:PORT` in `.env` (not container DNS) |

---

## Files to Know

- `.env` — Your local secrets (don't commit)
- `.env.example` — Template for team
- `DEVELOPMENT.md` — Full workflow guide
- `ARCHITECTURE.md` — Diagrams + mapping
- `compose.yaml` — Docker stack definition

---

**Questions?** Check `DEVELOPMENT.md` or `ARCHITECTURE.md` 🚀

