# UAT Report

## Execution Summary

- Execution date: 2026-07-22
- Mode: regression-backed UAT rehearsal using automated evidence
- Total cases: 16
- PASS: 15
- BLOCKED: 1
- FAIL: 0

## Result Summary

| Area | Result |
|---|---|
| Gamer registration/login/current user/profile | PASS |
| Station Kiosk QR/session | PASS |
| Smart Station / MQTT mock orchestration | PASS |
| Wallet / Order | PASS |
| Staff F&B queue | PASS |
| LFG / Invitation / Lobby / Chat | PASS |
| Voice fallback | BLOCKED |
| Staff Technical / Device alert | PASS |
| Notification | PASS |
| Dashboard / Export | PASS |
| Branch scope | PASS |
| Super Admin administration | PASS |

## Evidence Used

- `server/build/reports/tests/test/index.html`
- `server/src/test/java/demo/server/auth/AuthFlowTests.java`
- `server/src/test/java/demo/server/auth/UserAdminTests.java`
- `server/src/test/java/demo/server/branch/BranchStationManagementTests.java`
- `server/src/test/java/demo/server/game/GameProfileTests.java`
- `server/src/test/java/demo/server/session/QrPlaySessionTests.java`
- `server/src/test/java/demo/server/iot/DeviceCommandMqttTests.java`
- `server/src/test/java/demo/server/wallet/WalletBillingTests.java`
- `server/src/test/java/demo/server/ordering/FoodOrderTests.java`
- `server/src/test/java/demo/server/lfg/LfgLobbyTests.java`
- `server/src/test/java/demo/server/websocket/WebSocketSubscriptionAuthorizationTests.java`
- `server/src/test/java/demo/server/iot/IotDeviceAlertTests.java`
- `server/src/test/java/demo/server/notification/NotificationDeliveryTests.java`
- `server/src/test/java/demo/server/report/ReportServiceTests.java`
- `server/src/test/java/demo/server/social/SocialModerationTests.java`
- `Performance-Test-Report.md`

## Blocked Case

### UAT-010 Voice fallback

Blocked because the current environment keeps:
- `VITE_ENABLE_VOICE=false`
- `VOICE_PROVIDER=mock`

That is an acceptable limitation for this pass, but it is not a manual signoff for a real provider-backed voice session.

## Observations

1. The regression evidence is strong for login, QR/session, order, wallet, LFG/lobby, alert, notification, dashboard, export, and branch scope.
2. Voice remains environment-dependent and must be re-run when a real provider or an explicitly enabled mock/provider configuration is approved.
3. No critical blocking defect was found in the covered flows.

## Signoff Status

Conditional UAT ready for stakeholder review, with voice explicitly blocked until enabled-provider evidence is available.

