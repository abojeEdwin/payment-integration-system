# Payment Integration System - Docker Dev Setup

This repository is configured for phase-2 local development with:
- PostgreSQL (replacing H2 runtime usage)
- Kafka (KRaft mode)
- Auth, Payment, and Webhook services running in Docker
- PgAdmin and Kafka UI for local inspection

## Prerequisites

- Docker Engine + Docker Compose v2

## Quick Start

1. Copy env template:

```bash
cp .env.example .env
```

2. Start the full stack:

```bash
docker compose -f compose.yaml up -d --build
```

3. Check status:

```bash
docker compose -f compose.yaml ps
```

4. Follow logs (optional):

```bash
docker compose -f compose.yaml logs -f --tail=200
```

## Endpoints

- Auth service: `http://localhost:8081`
- Payment service: `http://localhost:8082`
- Webhook service: `http://localhost:8083`
- PgAdmin: `http://localhost:8080`
- Kafka UI: `http://localhost:8090`
- Kafka (host): `localhost:9092`
- Postgres (host): `localhost:5432`

## Notes

- Postgres initialization script creates `auth_db`, `payment_db`, `webhook_db` and roles on first container startup.
- If `5432` is busy on your machine, set `POSTGRES_HOST_PORT=5433` in `.env`.
- If Postgres volume already exists, initialization scripts are not re-run. To reset:

```bash
docker compose -f compose.yaml down -v
```

Then start again with `up -d --build`.

