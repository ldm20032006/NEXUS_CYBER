# API Documentation

Swagger UI is available at `/swagger-ui.html` and OpenAPI JSON at `/v3/api-docs` when the backend is running.

All endpoints are under `/api/v1` unless noted. Responses use DTOs and the common API/error envelope.

## Auth

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Register gamer |
| POST | `/auth/login` | Login by email/phone |
| POST | `/auth/refresh-token` or `/auth/refresh` | Rotate refresh token |
| POST | `/auth/logout` | Logout one device |
| POST | `/auth/logout-all` | Logout all devices |
| POST | `/auth/forgot-password` | Request reset |
| POST | `/auth/reset-password` | Reset password |
| POST | `/auth/change-password` | Change password |
| GET | `/auth/me` | Current user |

## Branch, Station, Admin Catalog

| Method | Path | Purpose |
|---|---|---|
| POST/GET | `/admin/branches` | Create/list branches |
| PUT/DELETE | `/admin/branches/{id}` | Update/soft delete branch |
| POST/GET | `/admin/zones` | Create/list zones |
| PUT/DELETE | `/admin/zones/{id}` | Update/soft delete zone |
| POST/GET | `/admin/stations` | Create/list stations |
| PATCH/DELETE | `/admin/stations/{id}` | Update/soft delete station |
| POST | `/admin/stations/{id}/credentials` | Issue station secret once |
| POST | `/admin/stations/{id}/credentials/rotate` | Rotate credential |
| POST | `/admin/stations/{id}/credentials/revoke` | Revoke credential |
| POST | `/stations/{id}/heartbeat` | Station heartbeat |

## Gamer Profile and Game

| Method | Path | Purpose |
|---|---|---|
| GET/PUT | `/profiles/me` | Get/update profile |
| POST | `/profiles/me/avatar` | Store avatar URL |
| GET/POST | `/profiles/me/game-profiles` | List/create game profile |
| PUT/DELETE | `/profiles/me/game-profiles/{id}` | Update/delete game profile |
| GET/PUT | `/profiles/me/station-preference` | Get/update station preference |
| POST/GET | `/admin/games` | Create/list games |
| PUT/DELETE | `/admin/games/{id}` | Update/soft delete game |
| POST | `/admin/games/{id}/ranks` | Add rank |
| POST | `/admin/games/{id}/roles` | Add role |

## QR and Session

| Method | Path | Purpose |
|---|---|---|
| POST/GET | `/qr-sessions` | Create QR session/list alias root |
| GET | `/qr-sessions/{id}` | Inspect QR status |
| POST | `/qr-sessions/{id}/confirm` | Confirm QR |
| POST | `/qr-sessions/{id}/cancel` | Cancel QR |
| GET | `/sessions/current` | Current session |
| GET | `/sessions/history` | Session history |
| POST | `/sessions/{id}/end` | End session |

## Wallet and Payment

| Method | Path | Purpose |
|---|---|---|
| GET | `/wallets/me` | Wallet overview |
| GET | `/wallets/me/transactions` | Wallet transaction history |
| POST | `/admin/wallets/{userId}/adjustments` | Admin adjustment with reason |
| POST | `/admin/wallet-transactions/{transactionId}/refund` | Refund transaction |
| POST | `/payments/topups` | Create top-up transaction |
| POST | `/payments/webhooks/mock` | Mock provider webhook |

## Menu and Orders

| Method | Path | Purpose |
|---|---|---|
| GET | `/menu/categories` | Public branch menu categories |
| GET | `/menu/items` | Public branch menu items |
| GET | `/menu/items/{id}` | Public menu item detail |
| POST/GET | `/admin/menu/categories` | Create/update categories |
| POST/PUT | `/admin/menu/items` | Create/update items |
| POST | `/orders` | Create order |
| GET | `/orders/me` or `/orders/my-orders` | Gamer orders |
| GET | `/orders/{id}` | Order detail |
| POST | `/orders/{id}/cancel` | Gamer cancel |
| GET | `/staff/orders` | Staff queue |
| GET | `/staff/orders/{id}` | Staff order detail |
| PATCH | `/staff/orders/{id}/status` | Staff state transition |
| POST | `/staff/orders/{id}/cancel` | Staff cancel |

## Social, LFG, Lobby

| Method | Path | Purpose |
|---|---|---|
| POST/DELETE/GET | `/social/blocks` | Block, unblock, list blocks |
| POST/GET | `/social/reports` | Report/list my reports |
| GET | `/social/radar/users` | Radar user search |
| POST/GET | `/lfg/signals` | Create/search LFG |
| GET | `/lfg/signals/me` | Current user's LFG |
| PUT/DELETE | `/lfg/signals/{id}` | Update/cancel LFG |
| POST | `/lfg/signals/{id}/renew` | Renew LFG |
| POST | `/team-invitations` | Send invitation |
| GET | `/team-invitations/received` | Received invitations |
| GET | `/team-invitations/sent` | Sent invitations |
| PATCH | `/team-invitations/{id}/accept` | Accept |
| PATCH | `/team-invitations/{id}/reject` | Reject |
| PATCH | `/team-invitations/{id}/cancel` | Cancel |
| POST/GET | `/lobbies` | Create/get lobby |
| DELETE | `/lobbies/{id}` | Disband lobby |
| POST/GET | `/lobbies/{id}/messages` | Chat |
| POST | `/lobbies/{id}/voice/token` | Issue short-lived voice token for active lobby member |
| POST | `/lobbies/voice/webhooks/mock` | Development mock voice webhook with HMAC signature |

## IoT, Notification, Reports

See Swagger for full schemas. Endpoint groups include `/admin/devices`, `/iot/devices`, `/staff/device-alerts`, `/iot/commands/ack`, `/notifications`, `/admin/dashboard/overview`, `/admin/reports/*`, and `/admin/audit-logs`.

## Headers

| Header | Purpose |
|---|---|
| `Authorization: Bearer <token>` | User JWT |
| `Idempotency-Key` | Required/recommended for mutation retries |
| `X-Correlation-ID` | Optional client correlation ID |
| Station credential headers | Used by kiosk/station endpoints; never expose secret after issue |

## Voice Chat Foundation

Voice uses `VoiceProviderPort`; the current implementation is a development mock provider and does not implement a voice codec or select a real provider. Tokens are short-lived, bound to user ID and lobby ID, and only active lobby members can request them. If the provider is unavailable, the API returns `VOICE_UNAVAILABLE`; Lobby and text chat remain available.

Mock webhook headers:

| Header | Purpose |
|---|---|
| `X-Voice-Timestamp` | Replay-window timestamp |
| `X-Voice-Signature` | HMAC-SHA256 of `timestamp.rawBody` |
