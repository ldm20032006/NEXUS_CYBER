# NEXUS Database Schema Plan

## Foundation Rules

- Primary key: `uuid` using PostgreSQL `gen_random_uuid()`.
- Timestamps: `timestamptz`, stored in UTC.
- Naming: `snake_case` table, column, index, constraint names.
- Standard mutable table columns: `id`, `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted`, `deleted_at`, `version`.
- Soft delete: use `deleted` and `deleted_at`; do not hard-delete users, stations, devices, menu items, or other business records unless explicitly documented.
- Optimistic locking: `version bigint NOT NULL DEFAULT 0`.
- Foreign keys: explicit `fk_<from_table>__<to_table>` names.
- Unique constraints: explicit `uk_<table>_<columns>` names.
- Indexes: explicit `ix_<table>_<columns>` names.
- Enum columns: store as text-compatible values matching Java `EnumType.STRING`; avoid ordinal values.
- Monetary values: `numeric(19,2)`, currency as ISO-style text such as `VND`.
- Large event/history payloads: use `jsonb` only for immutable snapshots or integration payloads.

## Delete Behavior

- Reference data used by history records should use `ON DELETE RESTRICT`.
- Child rows owned by an aggregate may use `ON DELETE CASCADE` only when the aggregate is not append-only and hard deletion is explicitly allowed.
- User-facing records should prefer soft delete over physical delete.
- History/audit records must never cascade-delete from business records.

## Append-Only Tables

The following tables are append-only by design:

- `wallet_transactions`
- `audit_logs`
- `alert_history`
- `command_history`

Append-only means no update or delete API, no mutable business fields, and corrections must be represented by a new compensating row. Retention/archival jobs may be added later with explicit audit and backup requirements.

## Module Schema Order

1. Foundation: PostgreSQL extension, migration infrastructure, base column conventions.
2. Auth: `users`, `roles`, `permissions`, `user_roles`, `refresh_tokens`, `password_reset_tokens`, token blacklist if required by the auth implementation.
3. Branch: `branches`, `zones`, `stations`, station credentials.
4. Catalog/Profile: `games`, `game_ranks`, `game_roles`, `gamer_profiles`, `gamer_game_profiles`, `station_preferences`.
5. Audit foundation: `audit_logs` append-only, correlation ID indexes.
6. QR/Session: `qr_login_sessions`, `play_sessions`, idempotency constraints.
7. Wallet/Billing: `wallets`, `wallet_transactions` append-only.
8. Ordering: `menu_categories`, `menu_items`, `food_orders`, `order_items`.
9. Social/LFG/Lobby: `user_blocks`, `user_reports`, `lfg_signals`, `team_invitations`, `lobbies`, `lobby_members`, `lobby_messages`.
10. Realtime/Notification: `notifications`, delivery metadata if needed.
11. IoT: `iot_devices`, `device_commands`, `command_history`, `device_telemetry`, `device_alerts`, `alert_history`.
12. Reports: aggregate views/materialized views only after transactional schemas stabilize.

## Per-Module Index Guidance

- Auth: unique `email`, unique nullable `phone`, token hash indexes, role code unique.
- Branch: unique branch code, unique zone code per branch, unique station code and station number per branch.
- Profile/Game: unique game slug, rank/role code per game, one gamer game profile per user/game.
- QR/Session: unique QR nonce, partial unique active session per user and station.
- Wallet: unique wallet per user, append-only transaction indexes by wallet/time/reference.
- Ordering: menu item code per branch, order indexes by branch/status/created_at.
- LFG/Lobby: active signal indexes by branch/game/rank/role/zone, unique active lobby member per lobby/user.
- IoT: unique device serial, telemetry indexes by device/time, alert indexes by branch/status/severity.
- Notification/Audit: user/read indexes for notifications, actor/branch/action/occurred_at indexes for audit.

## Migration Policy

- Never modify an applied migration.
- Use one migration per coherent module step.
- Keep DDL reversible in review notes even when Flyway migration files are forward-only.
- Do not create schema objects for a module before its entity/API/service contract is ready.
- Production must run Flyway before Hibernate validation.
