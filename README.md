# NEXUS Smart Cyber Esports

NEXUS is a modular monolith web platform for cyber esports centers. It includes a Spring Boot backend, React/Vite PWA frontend, PostgreSQL persistence, Redis-ready resilience abstractions, WebSocket realtime delivery, and MQTT mock integration for Smart Station workflows.

Canonical requirements are kept in:

- [SRS](md/SRS_NEXUS_WEB_FULLSTACK.md)
- [Detailed Scope](md/NEXUS_Project_Scope_Chi_Tiet.md)

Do not create alternate SRS or Scope files. All implementation and release documents below trace back to those two source documents.

## Prerequisites

- Java 21.
- Node.js 24 and npm.
- Docker Desktop or compatible Docker Engine with Compose plugin.
- PostgreSQL 16 for non-Docker local backend runs.
- Redis 7 and MQTT broker only when validating distributed cache, realtime, or IoT flows outside test/mock mode.

## Ports

| Service | Default | Notes |
|---|---:|---|
| Backend | 8080 | Spring Boot API, Swagger, Actuator |
| Frontend dev | 5173 | Vite dev server |
| Frontend Docker/Nginx | 8088 | Static frontend and `/api`, `/ws` proxy |
| PostgreSQL | 5432 | Docker Compose service |
| Redis | 6379 | Docker Compose service |
| MQTT | 1883 | Docker Compose Mosquitto, local mock only |

## Environment Variables

Use [.env.example](.env.example) as a template. Never commit `.env`.

Required backend variables:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC PostgreSQL URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret, must be supplied by environment |
| `POSTGRES_PASSWORD` | Docker PostgreSQL password |
| `PAYMENT_WEBHOOK_SECRET` | Mock webhook signature secret for local testing |
| `VOICE_WEBHOOK_SECRET` | Mock voice webhook signature secret for local testing |

Important runtime switches:

| Variable | Default | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` in Compose | `prod` validates schema |
| `JPA_DDL_AUTO` | `validate` in Compose | Production must not auto-create schema |
| `PAYMENT_PROVIDER` | `mock` | No real payment gateway selected |
| `VOICE_PROVIDER` | `mock` | No real voice provider selected |
| `MQTT_PROVIDER` | `mock` | MQTT device integration is mock/dev by default |
| `NOTIFICATION_PUSH_PROVIDER` | `mock` | Browser push foundation without real provider |
| `NOTIFICATION_EMAIL_PROVIDER` | `mock` | Email port with mock/dev adapter |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8088` | Whitelist for browser clients |

Frontend variables:

| Variable | Default | Notes |
|---|---|---|
| `VITE_API_BASE_URL` | `/api/v1` | Use relative URL behind Nginx |
| `VITE_WS_URL` | `/ws` | WebSocket/SockJS endpoint |
| `VITE_APP_NAME` | `NEXUS Smart Cyber Esports` | App display name |
| `VITE_ENABLE_PWA` | `true` | Enables PWA registration |
| `VITE_ENABLE_VOICE` | `false` | Shows Lobby voice UI only when enabled |

## Run Local

Backend with an existing PostgreSQL database:

```powershell
cd server
.\gradlew.bat bootRun
```

Frontend:

```powershell
cd client
npm install
npm run dev
```

Swagger is available at `http://localhost:8080/swagger-ui.html` when the backend is healthy.

## Run Docker

Create a local `.env` from [.env.example](.env.example), replace placeholders with local-only values, then run:

```powershell
docker compose up --build
```

Open:

- Frontend: `http://localhost:8088`
- Backend health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

The Docker stack includes PostgreSQL, Redis, Mosquitto MQTT, backend, and frontend/Nginx. Mosquitto is configured for local development only; production must use TLS and gateway credentials.

## Test

Backend:

```powershell
cd server
.\gradlew.bat clean build
```

Frontend:

```powershell
cd client
npm run lint
npm test
npm run build
npm run test:coverage
npm run test:pwa
```

## Demo Accounts

No production seed password is documented or committed. Demo users must be created through `/api/v1/auth/register` or by an approved local seed script that is never enabled in production.

Recommended demo roles to create manually:

| Role | Purpose |
|---|---|
| `GAMER` | Gamer PWA, QR, session, wallet, order, LFG |
| `STAFF_FNB` | Staff order queue |
| `STAFF_TECHNICAL` | Device alert queue |
| `BRANCH_ADMIN` | Branch-scoped administration |
| `SUPER_ADMIN` | System administration |
| `STATION_CLIENT` | Station kiosk credential flow |

## Troubleshooting

| Symptom | Check |
|---|---|
| Backend cannot start locally | PostgreSQL must be running and `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` must match |
| Flyway/validation failure | Run against a clean database or confirm migrations V1-V14 are applied in order |
| 401/403 from frontend | Verify access token, refresh flow, role, permission, and branch scope |
| Swagger not reachable through Docker | Check backend health first and Nginx proxy configuration |
| WebSocket does not connect | Check `/ws`, JWT or station credential, and reverse proxy Upgrade headers |
| QR camera unavailable | Browser requires HTTPS or localhost and explicit camera permission |
| Docker command unavailable | Install Docker Desktop or a compatible Docker Engine with Compose plugin |

## Documentation

- [System Architecture](System-Architecture.md)
- [System Flow](System-Flow.md)
- [Database Design](Database-Design.md)
- [API Documentation](API-Documentation.md)
- [Requirement Traceability Matrix](Requirement-Traceability-Matrix.md)
- [Deployment Guide](Deployment-Guide.md)
- [Disaster Recovery Guide](Disaster-Recovery-Guide.md)
- [Monitoring Guide](Monitoring-Guide.md)
- [Postman Guide](Postman-Guide.md)
- [Postman Collection](postman/NEXUS.postman_collection.json)
- [Test Plan](Test-Plan.md)
- [Test Report](Test-Report.md)
- [Security Test Report](Security-Test-Report.md)
- [Performance Test Report](Performance-Test-Report.md)
- [UAT Plan](UAT-Plan.md)
- [UAT Test Cases](UAT-Test-Cases.md)
- [UAT Report](UAT-Report.md)
- [Error Log](Error-Log.md)
- [Changelog](Changelog.md)
- [Demo Script](Demo-Script.md)
- [Prompt Chain](Prompt-Chain.md)
- [Contributing](CONTRIBUTING.md)
