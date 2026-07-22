# Test Report

## Latest Baseline

Date: 2026-07-21

| Area | Command | Result |
|---|---|---|
| Backend | `.\gradlew.bat clean build` | PASS |
| Frontend lint | `npm run lint` | PASS |
| Frontend unit/component | `npm test` | PASS, 13 files, 47 tests |
| Frontend build | `npm run build` | PASS |
| Documentation inventory | File existence and Postman JSON parse | PASS |
| Docker CLI check | `docker --version` | FAIL: Docker CLI unavailable |

## Voice Foundation Tests

| Scenario | Evidence | Result |
|---|---|---|
| Non-member cannot request token | `LfgLobbyTests` | PASS |
| Token is bound to user/lobby and has short TTL | `LfgLobbyTests` | PASS |
| Left member cannot request new token | `LfgLobbyTests` | PASS |
| Provider failure keeps lobby/text chat available | `LfgLobbyTests` | PASS |
| Webhook bad signature/replay | `LfgLobbyTests` | PASS |
| Lobby disband closes voice channel when supported | `LfgLobbyTests` | PASS |

## Observations

- Backend build generated JaCoCo reports.
- Backend emitted a deprecation warning in `GlobalExceptionHandler`.
- Frontend production build emitted a chunk-size warning for the main bundle.
- PWA plugin emitted an `inlineDynamicImports` deprecation warning.

## Coverage Status

Backend coverage artifacts are generated under `server/build/reports/jacoco/test/html`.
Frontend coverage should be generated with `npm run test:coverage`.

## Not Executed In This Report

- Docker Compose runtime smoke was not executed because Docker CLI is unavailable in this environment.
- Real payment, real email, real push, and real MQTT device integrations are not configured; mock/dev adapters are documented.
