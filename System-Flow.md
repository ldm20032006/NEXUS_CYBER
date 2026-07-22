# System Flow

Source requirements:

- [SRS](md/SRS_NEXUS_WEB_FULLSTACK.md)
- [Detailed Scope](md/NEXUS_Project_Scope_Chi_Tiet.md)

## Auth

```mermaid
sequenceDiagram
  participant U as User
  participant FE as Frontend
  participant BE as Backend
  participant DB as PostgreSQL
  U->>FE: Register/Login
  FE->>BE: /api/v1/auth/*
  BE->>DB: Validate user, role, token family
  BE-->>FE: Access token, refresh token, current user DTO
```

Refresh token rotation revokes the used token and detects reuse through token family rules. Locked or inactive users cannot authenticate.

## QR Login and Session

```mermaid
sequenceDiagram
  participant K as Station Kiosk
  participant G as Gamer
  participant BE as Backend
  participant DB as PostgreSQL
  participant WS as WebSocket
  K->>BE: Create QR with station credential
  BE-->>K: qrSessionId, nonce, expiresAt
  G->>BE: Confirm QR
  BE->>DB: Lock QR, station, gamer/session constraints
  BE->>DB: Create PlaySession, mark station OCCUPIED
  BE->>WS: Publish session event after commit
```

QR TTL is at most 60 seconds and QR is single-use. Backend remains the source of validation.

## Order

```mermaid
flowchart TD
  ActiveSession[Active session required] --> Menu[Load branch menu]
  Menu --> Cart[Client cart]
  Cart --> Backend[Create order with Idempotency-Key]
  Backend --> Price[Server calculates price snapshot]
  Price --> Stock[Lock stock and prevent oversell]
  Stock --> Payment{Payment policy}
  Payment -->|WALLET| Wallet[Wallet debit/refund policy]
  Payment -->|PAY_AT_COUNTER| Queue[Staff queue]
  Queue --> State[NEW to ACCEPTED to PREPARING to READY to DELIVERED]
```

Client prices are display-only. Backend snapshots `itemName`, `unitPrice`, `quantity`, and `subtotal`.

## LFG, Invitation, Lobby, Chat

LFG requires an active session and valid game profile. Matching filters branch, game, rank, role, zone, and user block rules. Invitations expire after 60 seconds and accept is concurrency-safe. Lobby visibility and chat are limited to members.

## IoT and Smart Station

Preference application creates device commands with command/correlation IDs. ACK handling is idempotent; timeout and retry are limited. Critical alerts can block unsafe mechanical commands but normal IoT failure must not block a play session.

## Notification

Domain events produce in-app notifications and WebSocket messages. Browser push and email are foundations with mock/dev adapters until real providers are configured.

## Reporting

Dashboard/report APIs use aggregate queries/projections and include filter timezone, from/to, and generatedAt. CSV export follows the same filters and branch scope.
