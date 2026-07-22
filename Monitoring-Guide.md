# Monitoring Guide

## What Runs For Real In This Repo

- Spring Boot Actuator endpoints: `health`, `info`, `metrics`
- Health probes: liveness and readiness
- Correlation ID propagation through REST, WebSocket, domain events, and MQTT-related paths
- Structured console logging with correlation ID in the log pattern
- Micrometer meters already used by services and background jobs

## What Is Still Sample Or Infrastructure-Dependent

- Prometheus scraping via Micrometer registry
- Grafana dashboards
- Loki/ELK centralized log shipping
- External alert routing
- Long-term retention and log aggregation policies

These are supported by the code and config shape, but they still require a chosen deployment stack for scraping, storage, and visualization.

## Endpoints

| Endpoint | Access | Purpose |
|---|---|---|
| `/actuator/health` | Public | Overall health |
| `/actuator/health/liveness` | Public | Liveness probe |
| `/actuator/health/readiness` | Public | Readiness probe |
| `/actuator/info` | Public | Build/app info |
| `/actuator/metrics` | Authenticated | Runtime metrics |
| `/actuator/prometheus` | Authenticated or internal-network only | Prometheus scrape endpoint when the Prometheus registry is enabled |

## Correlation ID

- Header: `X-Correlation-ID`
- Generated when missing
- Returned on every REST response
- Propagated to:
  - audit records
  - domain event envelopes
  - WebSocket message metadata
  - MQTT command/ack paths where applicable

Do not use correlation ID as a business identifier.

## Logging

Current console pattern includes:
- timestamp
- level
- thread
- logger
- correlation ID
- message

Do not log:
- access tokens
- refresh tokens
- passwords
- JWT secrets
- station secrets
- webhook signatures

## Business Metrics

The application already emits or can emit these without high-cardinality labels:

| Metric | Notes |
|---|---|
| Login success/failure | Use counters only, avoid email/user labels |
| Rate-limit rejection | Count by action type |
| Active session | Gauge or counter on lifecycle transitions |
| QR confirm duration | Timer |
| Order creation/status duration | Timer |
| Wallet mutation failure | Counter by reason |
| Active WebSocket connection | Gauge |
| MQTT reconnect | Counter |
| Device heartbeat timeout | Counter |
| Open critical alert | Gauge |
| Notification latency | Timer |
| Dashboard query duration | Timer |
| External provider error | Counter by provider and error class |

## Alerting Suggestions

| Alert | Trigger |
|---|---|
| Backend unhealthy | `health` or `readiness` DOWN |
| Database failure | DB health DOWN or connection errors spike |
| Redis unavailable | Redis health DOWN when Redis is enabled |
| MQTT disconnected | MQTT health DOWN or reconnect loop exceeds baseline |
| Critical device alert | Any open critical alert for a meaningful duration |
| Payment webhook failure | Signature/replay/idempotency failures above normal |
| Error rate breach | 5xx rate above SLA baseline |
| P95 latency breach | SLO P95 above target |

## Sample Prometheus / Grafana Notes

When `micrometer-registry-prometheus` is on:
- scrape `/actuator/prometheus` from an internal-only Prometheus target
- keep the endpoint off the public internet
- use Grafana to build panels for:
  - request latency
  - request rate
  - error rate
  - active sessions
  - webhook failures
  - MQTT reconnects

This repository does not ship a production Prometheus/Grafana stack; that remains a deployment concern.

## Operational Checks

1. `GET /actuator/health`
2. `GET /actuator/health/readiness`
3. `GET /actuator/health/liveness`
4. `GET /actuator/info`
5. Verify `X-Correlation-ID` is present on a sample REST request
6. Verify logs carry the same correlation ID
7. Verify business metrics are emitted without PII labels

## Known Risks

- Centralized logging is not bundled in the repo.
- Prometheus/Grafana are not bundled in the repo.
- High-cardinality metrics must be avoided in implementation and dashboards.
- Redis/MQTT/provider health checks are environment-dependent.
