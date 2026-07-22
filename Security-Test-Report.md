# Security Test Report

## Scope

Reviewed and validated the current security surface without adding business features:

- Login brute force
- Account enumeration
- JWT tampering, expiry, replay
- Refresh token reuse and logout token reuse
- Role, permission, and branch-scope bypass
- IDOR on UUIDs and mass assignment
- SQL injection and XSS
- CORS and CSRF behavior
- WebSocket/STOMP authentication and subscription authorization
- Station credential authenticity
- MQTT topic spoofing and fake device messages
- Payment webhook replay and signature validation
- Voice webhook replay when voice is enabled
- Rate-limit bypass
- Sensitive data exposure, stack trace masking, secret leakage in source
- Dependency vulnerability scan coverage
- Actuator and Swagger exposure
- Traversal risk where uploads exist

MFA is not part of this MVP scope. It is recorded here as an accepted risk, not partially implemented.

## Summary

| Area | Status | Severity | Evidence |
|---|---|---:|---|
| JWT tampering / expiry / replay | PASS | High | Covered by auth token service and auth flow regression tests |
| Refresh token reuse / logout reuse | PASS | High | Covered by auth flow regression tests |
| Role / permission / branch scope / IDOR / mass assignment | PASS | High | Covered by admin, branch, and WebSocket authorization tests |
| SQL injection / stored XSS / reflected XSS | PASS | High | Input validation, DTO binding, and output masking checks |
| CORS policy | PASS | Medium | `FoundationWebTests.corsAllowsConfiguredOriginsAndRejectsUnknownOrigins` |
| CSRF strategy | PASS | Medium | Stateless API with CSRF disabled intentionally; no cookie auth flow in scope |
| WebSocket auth / STOMP subscription | PASS | High | `WebSocketSubscriptionAuthorizationTests` |
| Station credential spoofing | PASS | High | Station credential and heartbeat tests |
| MQTT spoofing / fake device messages | PASS | High | Device command regression tests |
| Payment webhook replay / signature | PASS | High | `PaymentWebhookTests` |
| Voice webhook replay | PASS | High | Voice webhook regression tests |
| Rate-limit bypass on QR / invitation paths | RESOLVED | High | `ResilienceFilterTests.qrConfirmAndTeamInvitationAreRateLimitedOnTheirActualPaths` |
| Account enumeration on register | RESOLVED | Medium | Registration now returns generic duplicate message |
| Sensitive data exposure / stack traces | PASS | High | Standard error response format, masked exception handling |
| Dependency vulnerability scan | PARTIAL | Medium | Frontend `npm audit` returned 0 vulnerabilities; backend dedicated vuln scanner is not configured in repo |
| Actuator exposure | NEED_REVIEW | Medium | Health/info remain public by config; intended for this release unless production policy changes |
| Swagger exposure in production | NEED_REVIEW | Medium | OpenAPI and Swagger endpoints are currently public by config |
| MFA | ACCEPTED RISK | Medium | Not in MVP, not partially implemented |

## Resolved Findings

### 1. Rate-limit bypass on actual QR confirm and invitation endpoints

- Severity: High
- Status: Resolved

Root cause:

- The rate-limit filter originally matched legacy paths instead of the live routes.
- QR confirm traffic was not protected on `/api/v1/qr-sessions/{id}/confirm`.
- Invitation traffic was not protected on `/api/v1/team-invitations`.

Evidence:

- `server/src/main/java/demo/server/common/resilience/RateLimitFilter.java`
- `server/src/test/java/demo/server/resilience/ResilienceFilterTests.java`

Reproduction before fix:

1. Send repeated `POST /api/v1/qr-sessions/{uuid}/confirm` requests without authentication.
2. Send repeated `POST /api/v1/team-invitations` requests without authentication.
3. The actual limiter was not applied to the live endpoints because the filter matched the wrong path prefixes.

Fix:

- Updated the matcher to include the live endpoints.
- Kept legacy path compatibility where it already existed.

Regression test:

- `ResilienceFilterTests.qrConfirmAndTeamInvitationAreRateLimitedOnTheirActualPaths`

Observed post-fix behavior:

- Requests still flow through the auth layer as expected.
- Rate-limit headers are present on the live routes.
- The configured threshold returns `429 Too Many Requests` after the limit is exceeded.

### 2. Account enumeration on registration

- Severity: Medium
- Status: Resolved

Root cause:

- Duplicate email and duplicate phone previously returned different messages.

Evidence:

- `server/src/main/java/demo/server/service/auth/AuthService.java`
- `server/src/test/java/demo/server/auth/AuthFlowTests.java`

Fix:

- Registration now returns a generic duplicate-account message.

Regression test:

- `AuthFlowTests` registration duplicate assertion

## Accepted / Deferred Risks

### Swagger exposure in production

- Severity: Medium
- Status: Need review

Current state:

- `/v3/api-docs/**` and `/swagger-ui/**` are permitted by security configuration.

Why it remains:

- It is useful in non-production and test environments.
- The repository does not currently have a production-gating policy for these endpoints.

Operational note:

- If production policy changes, gate Swagger/OpenAPI by profile or property and add a regression test.

### Actuator exposure

- Severity: Medium
- Status: Need review

Current state:

- `health` and `info` are public.

Why it remains:

- Health checks are typically required for deploy and orchestrator probes.

Operational note:

- If a stricter production policy is required, restrict actuator exposure by profile or management port and document the probe path.

### MFA

- Severity: Medium
- Status: Accepted risk

Reason:

- MFA is not part of the MVP contract in this release.
- No partial MFA flow was introduced.

### Backend dependency vulnerability scan

- Severity: Medium
- Status: Partial

Current state:

- Frontend dependency audit was executed and returned `0 vulnerabilities`.
- The backend build does not currently include a dedicated dependency vulnerability scanner.

Operational note:

- Add a dedicated scanner before claiming automated backend dependency coverage.

## Test Evidence

Executed successfully in this pass:

- `./gradlew.bat test --tests demo.server.resilience.ResilienceFilterTests --tests demo.server.auth.AuthFlowTests --tests demo.server.foundation.FoundationWebTests --tests demo.server.websocket.WebSocketSubscriptionAuthorizationTests`
- `./gradlew.bat clean build`
- `npm audit --audit-level=high`

## Regression Coverage

- `AuthFlowTests`
- `ResilienceFilterTests`
- `FoundationWebTests`
- `WebSocketSubscriptionAuthorizationTests`
- Existing payment, MQTT, QR/session, and auth test suites already cover the other listed controls

## Notes

- No real credentials, tokens, or secrets were introduced into code or documentation.
- Error responses continue to use the standard masked format; stack traces are not exposed to clients.
