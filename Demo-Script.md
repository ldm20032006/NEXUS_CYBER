# Demo Script

## Purpose

Show the working NEXUS flows with the same scope used in the UAT regression evidence.

## Demo Order

1. Open the backend health page and confirm `UP`.
2. Register a Gamer account, login, and open current user.
3. Open Gamer profile and station preference.
4. As Branch Admin or Super Admin, create branch, zone, and station.
5. As Station Kiosk, generate a QR and show that it does not expose JWT or secret material.
6. Gamer confirms the QR and starts a session.
7. Show current session, then end the session.
8. Show Smart Station progress using the mock/dev MQTT path.
9. Open wallet overview and transaction history.
10. Create a food order and show staff queue progression.
11. Open LFG, send an invitation, join a lobby, and show chat history/realtime updates.
12. Resolve a device alert and show its history.
13. Show the notification center and realtime delivery.
14. Open the dashboard, apply branch/timezone filters, and export CSV.
15. Show branch scope boundaries by switching to a different branch-scoped record.
16. Show role/admin boundaries for Branch Admin and Super Admin.

## Role Checkpoints

### Gamer

- Register/login
- Current user
- Game profile
- Station preference
- QR login
- Session
- Wallet
- Order
- LFG / Invitation / Lobby
- Notifications

### Staff F&B

- Order queue
- Accept / prepare / ready / deliver
- Cancel with reason where policy allows

### Staff Technical

- Device list
- Alerts
- Resolve / close / reopen

### Branch Admin

- Branch / zone / station management
- KPI dashboard
- CSV export
- Branch scope enforcement

### Super Admin

- User and role administration
- Permission and escalation boundaries

### Station Kiosk

- Station credential auth
- QR generation and refresh
- Session start and status updates
- Smart Station progress

## Voice

- Voice is optional in this pass.
- Current local configuration defaults to mock/provider-disabled.
- If voice is enabled later, repeat the lobby voice token and webhook checks from the UAT voice case.

## Evidence To Show

- `UAT-Report.md`
- `UAT-Test-Cases.md`
- `Performance-Test-Report.md`
- `Security-Test-Report.md`
- `server/build/reports/tests/test/index.html`

## Demo Constraints

- Do not show tokens, passwords, secrets, or webhook signatures.
- If a flow is provider-dependent, say so explicitly instead of pretending it is live.
- If voice is disabled, state it as a known limitation and skip the live voice step.

