# Disaster Recovery Guide

## Objectives

| Item | Target |
|---|---|
| RPO | <= 15 minutes for PostgreSQL |
| RTO | <= 4 hours for MVP service restoration |
| Audit retention | >= 12 months |

## Backup Scope

- PostgreSQL: required, includes business data, audit, wallet, sessions, orders, alerts.
- Redis: optional for cache/ephemeral state; durable data must remain in PostgreSQL.
- MQTT: no durable business source of truth.
- Frontend: rebuildable from Git and CI artifact.
- Backend: rebuildable from Git and CI artifact.

## Restore Procedure

1. Freeze writes if partial outage risks data corruption.
2. Restore PostgreSQL from the latest verified backup.
3. Run Flyway validation only; do not auto-create schema in production.
4. Start backend with `SPRING_PROFILES_ACTIVE=prod`.
5. Start frontend/Nginx.
6. Validate health, auth, QR/session, wallet, order, IoT alert, notification, and report smoke tests.
7. Record incident details in [Error Log](Error-Log.md).

## Data Integrity Checks

- Wallet balance equals sum of non-reversed wallet transactions.
- No gamer or station has more than one active session.
- Append-only tables have no unexpected updates/deletes.
- Payment transactions with succeeded provider IDs are unique.
- Open alerts have no unintended duplicates.

## Drill Cadence

- Backup restore drill: monthly.
- Secret rotation drill: quarterly.
- Incident simulation: before production go-live and after major schema changes.
