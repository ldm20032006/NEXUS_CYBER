# Changelog

## 2026-07-21

- Added Lobby voice chat foundation with `VoiceProviderPort`, development mock provider, short-lived member tokens, voice webhook signature/replay/idempotency handling, frontend feature flag, and V15 migration.
- Added Docker deployment foundation: backend Dockerfile, frontend Dockerfile, Nginx config, Docker Compose, Mosquitto config, `.env.example`, and GitHub Actions CI.
- Updated frontend environment validation to support same-origin relative API and WebSocket URLs for Nginx deployment.
- Added release documentation set: architecture, flow, database, API, traceability, deployment, disaster recovery, monitoring, Postman, QA/UAT, security, performance, error log, demo script, prompt chain, and contributing guide.

## Earlier NEXUS implementation chain

- Foundation, database conventions, auth/RBAC, resilience abstractions, audit/event foundation.
- Branch/station, game/profile/preference, WebSocket, QR/session, wallet/billing/payment mock.
- Menu/order, social moderation, LFG/lobby/chat, IoT alert/command mock, notification, reporting, scheduling.
- Frontend architecture, gamer/station/staff/admin flows, PWA/responsive, review and baseline testing.
