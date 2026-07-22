# Database Design

Source requirements:

- [SRS](md/SRS_NEXUS_WEB_FULLSTACK.md)
- [Detailed Scope](md/NEXUS_Project_Scope_Chi_Tiet.md)

## Migration Inventory

| Version | File | Purpose |
|---|---|---|
| V1 | `V1__foundation_baseline.sql` | UUID, audit columns, common baseline |
| V2 | `V2__auth_identity_rbac.sql` | AppUser, roles, permissions, refresh/reset tokens |
| V3 | `V3__audit_log_append_only.sql` | Append-only audit log |
| V4 | `V4__branch_station_management.sql` | Branch, zone, station, station credential |
| V5 | `V5__game_gamer_profile_station_preference.sql` | Game, rank, role, gamer profile, preferences |
| V6 | `V6__qr_login_and_play_session.sql` | QR and play sessions |
| V7 | `V7__wallet_and_session_billing.sql` | Wallet, transactions, billing policy |
| V8 | `V8__payment_transactions.sql` | Payment transaction |
| V9 | `V9__menu_stock_food_order.sql` | Menu, stock MVP, food orders |
| V10 | `V10__social_moderation.sql` | User block/report |
| V11 | `V11__lfg_lobby_chat.sql` | LFG, invitation, lobby, chat |
| V12 | `V12__iot_device_telemetry_alert.sql` | IoT device, telemetry, alert |
| V13 | `V13__device_command_mqtt.sql` | Device command and command history |
| V14 | `V14__notification_delivery_push.sql` | Notifications and push subscriptions |

Applied migrations must not be edited. Add a new V15+ migration for future schema changes.

## Core Entity Groups

| Group | Entities |
|---|---|
| auth | AppUser, Role, Permission, RefreshToken, PasswordResetToken |
| branch | Branch, Zone, Station, StationCredential |
| gamer/game | GamerProfile, StationPreference, Game, GameRank, GameRole, GamerGameProfile |
| session | QrLoginSession, PlaySession, SessionBillingPolicy |
| wallet/payment | Wallet, WalletTransaction, PaymentTransaction |
| ordering | MenuCategory, MenuItem, FoodOrder, OrderItem |
| social/lfg/lobby | UserBlock, UserReport, LfgSignal, TeamInvitation, Lobby, LobbyMember, LobbyMessage |
| iot | IotDevice, DeviceTelemetry, DeviceAlert, AlertHistory, DeviceCommand, CommandHistory |
| notification/audit | Notification, NotificationDelivery, PushSubscription, AuditLog |

## Standards

- Primary IDs use UUID.
- Timestamps are stored in UTC.
- JPA enum fields use string semantics at the domain layer.
- Soft delete is used for mutable business records that must not be hard-deleted.
- `version` is used for optimistic concurrency on mutable aggregate roots.
- Production `spring.jpa.hibernate.ddl-auto` must be `validate`.

## Index and Constraint Policy

- Unique constraints protect email, phone, station code in branch, one wallet per gamer, one game profile per user/game, and provider transaction IDs.
- Foreign keys enforce ownership and branch/station relations.
- Business uniqueness is backed by both service validation and database constraints.
- Active-session limits require database constraints plus transaction/lock/idempotency checks.
- Append-only records must not expose update/delete business APIs.

## Delete Behavior

| Table type | Behavior |
|---|---|
| User, branch, station, menu item, device | Soft delete or disabled status |
| AuditLog, WalletTransaction, AlertHistory, CommandHistory | Append-only; no update/delete API |
| Session/order/chat history | Retained according to job retention policy |
