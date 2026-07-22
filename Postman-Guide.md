# Postman Guide

Collection: [postman/NEXUS.postman_collection.json](postman/NEXUS.postman_collection.json)

## Environment Variables

| Variable | Example |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `apiBase` | `{{baseUrl}}/api/v1` |
| `accessToken` | Set after login |
| `refreshToken` | Set after login |
| `idempotencyKey` | Generate per mutation retry family |

Do not store real tokens, passwords, station secrets, or production webhook secrets in exported collections.

## Recommended Order

1. Health.
2. Register/Login.
3. Current user.
4. Admin setup for branch, zone, station, game, menu.
5. Station credential and QR.
6. Gamer confirm QR and session.
7. Wallet top-up mock.
8. Order flow.
9. LFG/invitation/lobby/chat.
10. Device alert and command mock.
11. Notification and report.

## Auth

After login, set `accessToken` manually or with a Postman test script. Protected requests use:

```text
Authorization: Bearer {{accessToken}}
```

For retry-safe mutations, set:

```text
Idempotency-Key: {{idempotencyKey}}
```
