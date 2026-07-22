# UAT Plan

## Objective

Validate the end-to-end NEXUS workflows described in the SRS and Scope using evidence-backed regression results, then capture which user journeys are ready for stakeholder manual signoff and which are still environment-dependent.

## Scope

Roles in scope:
- Gamer
- Staff F&B
- Staff Technical
- Branch Admin
- Super Admin
- Station Kiosk

Flows in scope:
- Register/Login
- QR Login
- Session
- Smart Station
- LFG/Invitation/Lobby
- Voice fallback
- Order/Wallet
- Staff Order
- Device/Alert
- Notification
- Dashboard
- Export
- Branch Scope

## Execution Model

This pass is a regression-backed UAT rehearsal, not a live stakeholder session.

Evidence comes from:
- Backend automated tests in `server/src/test/java`
- Generated test report in `server/build/reports/tests/test/index.html`
- Performance evidence in `server/build/reports/performance/performance-results.json`
- Repository documentation and environment templates

## Entry Criteria

- Backend and frontend baseline evidence is available.
- Demo data can be created without production secrets.
- Mock/dev adapters are clearly identified.
- Known limitations are documented.

## Exit Criteria

- All in-scope cases are either PASS with evidence or explicitly BLOCKED with a reason.
- No case is marked PASS without actual result and evidence.
- Open blockers are visible and actionable.

## UAT Rules

- Do not expose tokens, passwords, station secrets, or webhook secrets.
- Do not treat mock providers as production integrations.
- Do not claim voice, payment, MQTT, email, or push as real providers unless they are actually configured.
- Use branch scope in every admin validation.

