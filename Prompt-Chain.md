# Prompt Chain

This document records the high-level implementation chain used for NEXUS. It is not a replacement for the SRS or Scope.

1. Baseline review of `server/`, `client/`, requirements, config, migrations, tests, README, Docker Compose, build files.
2. Foundation: BaseEntity, auditing, response envelope, exception handling, correlation ID, OpenAPI/Actuator, service interfaces.
3. Database conventions and minimal migrations.
4. Auth/RBAC/user administration.
5. Resilience abstractions: cache, rate limit, idempotency, lock, online state.
6. Audit and domain event foundation.
7. Branch/zone/station/station credentials.
8. Game/profile/station preference.
9. WebSocket authorization.
10. QR login and play session.
11. Wallet, transaction, billing policy.
12. Payment port and mock adapter.
13. Menu/order/staff order flow.
14. Social moderation.
15. LFG/invitation/lobby/chat.
16. IoT device/telemetry/alert.
17. Device command/MQTT mock/apply preference.
18. Notification foundation.
19. Reporting/KPI/export.
20. Scheduled jobs.
21. Backend review and coverage.
22. Frontend architecture and auth/profile/session/wallet.
23. QR scanner and Smart Station UI.
24. Radar/lobby/chat UI.
25. Menu/cart/checkout UI.
26. Station kiosk UI.
27. Staff order queue.
28. Staff device alert UI.
29. Admin management.
30. Admin reporting.
31. PWA and responsive hardening.
32. Frontend review and coverage.
33. Full-system validation.
34. Docker/CI deployment foundation.
35. Release documentation and QA handoff.
