# Deployment Guide

## Sources

- [SRS](md/SRS_NEXUS_WEB_FULLSTACK.md)
- [Detailed Scope](md/NEXUS_Project_Scope_Chi_Tiet.md)

## Local Docker Deployment

1. Copy `.env.example` to `.env`.
2. Replace placeholders with local-only values.
3. Run `docker compose up --build`.
4. Validate:
   - Frontend: `http://localhost:8088`
   - Backend health: `http://localhost:8080/actuator/health`
   - Swagger: `http://localhost:8080/swagger-ui.html`
   - Redis health: `docker compose exec redis redis-cli ping`
   - MQTT health: `docker compose exec mqtt mosquitto_pub -h localhost -p 1883 -t nexus/healthcheck -m ping`

## Services

| Service | Image/build | Health |
|---|---|---|
| postgres | `postgres:16-alpine` | `pg_isready` |
| redis | `redis:7-alpine` | `redis-cli ping` |
| mqtt | `eclipse-mosquitto:2` | Mosquitto publish check |
| backend | `server/Dockerfile` | `/actuator/health` |
| frontend | `client/Dockerfile` + Nginx | `/healthz` |

## Profiles

Spring profile policy:

- `local`: developer setup with mock integrations.
- `test`: automated test resources; no production secrets.
- `prod`: schema validation, externalized secrets, TLS at edge.

Docker Compose defaults to a local-stack topology but uses explicit environment values. Production should supply `.env` through a secret manager or deployment platform, not from source control.

## Nginx Routing

- `/api` proxies to backend REST.
- `/ws` proxies WebSocket/SockJS traffic.
- `/swagger-ui.html`, `/swagger-ui`, and `/v3/api-docs` proxy to backend documentation.
- Other routes use SPA fallback to `index.html`.

## Production Checklist

- Use HTTPS/TLS at the reverse proxy.
- Use managed PostgreSQL backups.
- Use Redis-backed implementations before multi-instance scale.
- Replace payment, email, push, and MQTT mock providers.
- Replace the voice mock provider with an approved real provider before production voice rollout.
- Configure MQTT TLS and gateway credentials.
- Set `JPA_DDL_AUTO=validate`.
- Do not enable production seed passwords.
- Store secrets outside Git.
- Run backend, frontend, and smoke tests before release.
