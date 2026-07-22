# NEXUS SMART CYBER ESPORTS
# PROMPT CHAIN A–Z HOÀN CHỈNH

Chuỗi này dùng tuần tự cho dự án NEXUS Fullstack Web/PWA + Spring Boot + PostgreSQL + Redis + WebSocket + MQTT.

## Hợp đồng kỹ thuật cố định

- Kiến trúc: Modular Monolith.
- Backend: Java 17, Spring Boot 3, Maven.
- Frontend: ReactJS, TypeScript, Vite.
- Database: PostgreSQL; migration bằng Flyway.
- Redis: cache, rate limit, idempotency, distributed lock và trạng thái tạm.
- Realtime: WebSocket/STOMP.
- IoT: MQTT qua adapter.
- ID: UUID.
- Timestamp: `Instant`, lưu UTC.
- Tiền: `BigDecimal`.
- API prefix: `/api/v1`.
- Không trả JPA Entity trực tiếp.
- Controller không chứa business logic.
- Quan hệ JPA dùng `LAZY` khi phù hợp.
- Enum dùng `EnumType.STRING`.
- Không hard delete dữ liệu nghiệp vụ quan trọng.
- Không sửa migration đã áp dụng; tạo migration mới.
- Không thêm dependency nếu chưa thật cần.
- Không tắt test để làm build pass.
- Không hardcode secret.

## Quy tắc kế thừa bắt buộc

Dán khối này ở đầu mọi prompt từ Prompt 01:

```text
Đây là bước tiếp theo trong chuỗi triển khai NEXUS.

Trước khi sửa:
1. Đọc code và kết quả của các bước trước.
2. Đọc `md/SRS_NEXUS_WEB_FULLSTACK.md`.
3. Đọc `md/NEXUS_Project_Scope_Chi_Tiet.md`.
4. Chạy build/test để xác nhận baseline.
5. Không triển khai module mới khi baseline đang lỗi chưa rõ nguyên nhân.

Trong khi sửa:
- Không tạo lại class, enum, DTO, repository, service, controller, config hoặc migration đã tồn tại.
- Mở rộng thành phần hiện có thay vì tạo bản trùng.
- Không đổi API contract cũ nếu không thật sự cần.
- Nếu buộc đổi contract: nêu lý do, cập nhật test, Swagger, Postman và frontend liên quan.
- Không sửa migration đã áp dụng.
- Không xóa code chưa hiểu.
- Không trả Entity trực tiếp.
- Không đưa business logic vào Controller.
- Không dùng mock để giả vờ tích hợp thật.
- Không hardcode secret/token/password.

Sau khi sửa:
1. Chạy build/test.
2. Không sang bước tiếp theo nếu chưa pass.
3. Báo cáo file tạo/sửa, API, migration, test, kết quả build, rủi ro và việc còn lại.
```

---

# PROMPT 00 — PROJECT AUDIT VÀ KẾ HOẠCH

```text
Bạn là Senior Solution Architect, Senior Java Developer, Senior React Developer và Technical Lead.

Đọc toàn bộ `server/`, `client/`, hai tài liệu SRS/Scope, cấu hình, migration, test, README, Docker Compose, `pom.xml`, `package.json`.

Không sửa code.

Thực hiện:
- Chạy baseline backend/frontend.
- Thống kê module, entity, DTO, API, migration và test hiện có.
- Phát hiện code/enum/endpoint/migration trùng.
- So sánh code với SRS/Scope.
- Lập Dependency Map.
- Gắn trạng thái DONE/PARTIAL/MISSING/BROKEN/NEED_REVIEW.
- Đề xuất thứ tự triển khai an toàn.
- Nêu xung đột giữa tài liệu và code.
```

---

# PROMPT 01 — BACKEND FOUNDATION

```text
Chuẩn hóa package Modular Monolith và foundation.

Thực hiện:
- `BaseEntity`: UUID, `createdAt`, `updatedAt`, `deleted`, `deletedAt`, `version`.
- JPA auditing, UTC, `EnumType.STRING`.
- `ApiResponse<T>`, `PageResponse<T>`, error format thống nhất.
- Global exception handler.
- Exception chuẩn: not found, duplicate, business rule, invalid transition, forbidden, unauthorized, token, external service, concurrency.
- Correlation ID filter.
- Logging và masking.
- OpenAPI/Actuator foundation.
- Tạo interface nền: `CurrentUserProvider`, `ClockProvider`, `DomainEventPublisher`, `AuditRecorder`.
- Không thêm nghiệp vụ.
- Test validation, error, UTC, correlation ID.
```

---

# PROMPT 02 — DATABASE CONVENTION VÀ FLYWAY BASELINE

```text
Chuẩn hóa quy ước database nhưng không tạo toàn bộ schema nghiệp vụ quá sớm.

Thực hiện:
- UUID PostgreSQL, UTC timestamp, naming, audit columns, version, soft delete.
- Kiểm tra `ddl-auto`; production dùng `validate`.
- Tạo baseline migration tối thiểu.
- Không sửa migration cũ.
- Lập Schema Plan cho từng module.
- Quy định index, unique constraint, FK, delete behavior.
- Append-only cho WalletTransaction, AuditLog, AlertHistory, CommandHistory.
- Testcontainers PostgreSQL chạy migration từ database rỗng.
```

---

# PROMPT 03 — AUTHENTICATION, AUTHORIZATION VÀ USER ADMIN

```text
Triển khai:
- Register Gamer.
- Login email/phone.
- JWT access token.
- Refresh token rotation, reuse detection, token family revoke.
- Logout một thiết bị và tất cả thiết bị.
- Forgot/reset/change password.
- Current User.
- RBAC, permission, branch scope.
- Lock/activate account.
- User/Staff administration.

Entity: AppUser, Role, Permission, RefreshToken, PasswordResetToken và mappings.

Role: GAMER, STAFF_FNB, STAFF_TECHNICAL, BRANCH_ADMIN, SUPER_ADMIN, STATION_CLIENT.

Yêu cầu:
- Unique email/phone.
- Password BCrypt/Argon2.
- Token hash, TTL, single-use.
- Branch Admin không tạo Super Admin.
- Method security, AuthenticationEntryPoint, AccessDeniedHandler.
- Migration riêng.
- Test đầy đủ auth, lock, refresh, scope, role escalation.
```

---

# PROMPT 04 — REDIS, RATE LIMIT, IDEMPOTENCY VÀ LOCK

```text
Tạo abstraction:
- CacheService.
- RateLimitService.
- IdempotencyService.
- DistributedLockService.
- OnlineStateService.

Áp dụng:
- Rate limit login, forgot/reset, QR confirm, invitation, order.
- Idempotency-Key với request fingerprint và trạng thái.
- Lock QR, session, wallet, payment callback, stock, invitation accept.
- Lock có timeout; DB constraint vẫn là lớp cuối.
- TTL và key prefix rõ.
- Chính sách khi Redis lỗi.
- Testcontainers Redis nếu khả thi.
```

---

# PROMPT 05 — AUDIT VÀ DOMAIN EVENT FOUNDATION

```text
Triển khai Audit sớm để module sau không tạo helper tạm.

AuditLog:
- actor, role, branch, action, resource, before/after, IP, user-agent, correlationId, timestamp.
- Append-only, mask sensitive data.
- Không có update/delete API.

Domain event:
- Event envelope có eventId, eventType, version, timestamp, correlationId, payload.
- In-process event cho Modular Monolith.
- Publish outward sau commit khi cần.
- Không dùng Kafka.
- Test immutable và masking.
```

---

# PROMPT 06 — BRANCH, ZONE, STATION VÀ STATION CREDENTIAL

```text
Triển khai Branch, Zone, Station, StationCredential và heartbeat.

Yêu cầu:
- CRUD, pagination, filter, branch scope.
- Branch có timezone, opening hours, payment policy.
- Station status AVAILABLE/OCCUPIED/OFFLINE/MAINTENANCE/DISABLED.
- Station code unique trong Branch.
- Credential Kiosk riêng, hash secret, rotate/revoke, secret chỉ trả một lần.
- Heartbeat và lastSeen UTC.
- Không hard delete.
- Audit và migration.
- Test status, scope, credential.
```

---

# PROMPT 07 — GAME, PROFILE, GAME PROFILE, PREFERENCE

```text
Triển khai Game, GameRank, GameRole, GamerProfile, GamerGameProfile, StationPreference.

Yêu cầu:
- Game CRUD Admin, active, maxLobbySize.
- Rank/Role phải thuộc đúng Game.
- Một Game Profile/user/game.
- Public Gamer DTO không lộ email/phone/balance.
- Desk 60–120, Chair 90–145, RGB `#RRGGBB`, brightness, DPI, night mode.
- Avatar hiện chỉ lưu URL nếu chưa có storage an toàn.
- Migration, audit và test validation/ownership/unique.
```

---

# PROMPT 08 — WEBSOCKET/STOMP FOUNDATION

```text
Triển khai `/ws`.

Yêu cầu:
- JWT cho user; Station Credential cho Kiosk.
- Authorization khi SUBSCRIBE:
  - user queue của chính mình,
  - station topic đúng station,
  - branch topic đúng branch,
  - lobby topic chỉ member.
- Topic chuẩn cho notification, station, order, alert, lobby.
- Event envelope có version.
- Không gửi Entity.
- Payload/rate limit, heartbeat, reconnect, eventId chống trùng.
- Redis Pub/Sub chỉ chuẩn bị nếu nhiều instance.
- Test subscription authorization.
```

---

# PROMPT 09 — QR LOGIN VÀ PLAY SESSION

```text
Triển khai QrLoginSession và PlaySession.

QR:
- stationId, qrSessionId, nonce, expiresAt.
- Không chứa JWT/secret.
- TTL tối đa 60 giây.
- Dùng một lần.

Session:
- PENDING/ACTIVE/PAUSED/COMPLETED/CANCELLED/TERMINATED.
- Mỗi Gamer và Station tối đa một Active.
- DB constraint + transaction + lock + idempotency.
- Station credential tạo QR.
- Confirm kiểm tra QR, nonce, account, station, wallet policy.
- Update station OCCUPIED; publish event sau commit; audit.
- Current/history/end session.
- Test expired, used, duplicate, race, state, scope.
```

---

# PROMPT 10 — WALLET VÀ SESSION BILLING

```text
Triển khai Wallet, WalletTransaction và SessionBillingPolicy.

Yêu cầu:
- Một Wallet/Gamer.
- Balance không âm.
- BigDecimal.
- Transaction append-only.
- Chỉ WalletService thay đổi balance.
- balanceBefore/balanceAfter.
- Lock row và idempotency.
- Refund tham chiếu giao dịch gốc.
- Admin adjustment có reason + audit.
- Billing theo Branch/Zone/Station, estimated/final cost, không double charge.
- Chưa tích hợp provider ở bước này.
- Test concurrency, refund, billing.
```

---

# PROMPT 11 — PAYMENT ADAPTER VÀ TOP-UP

```text
Không tự chọn cổng thanh toán thật.

Triển khai:
- PaymentGatewayPort.
- Mock adapter development.
- PaymentTransaction.
- Status PENDING/PROCESSING/SUCCEEDED/FAILED/CANCELLED/REFUNDED.
- Top-up request.
- Webhook verify signature, anti-replay, idempotent, cộng tiền một lần.
- Provider transaction ID unique.
- Không log secret/signature đầy đủ, không lưu card data.
- Ghi rõ mock nếu chưa có provider thật.
- Test callback trùng, replay, signature sai.
```

---

# PROMPT 12 — MENU, STOCK MVP VÀ FOOD ORDER

```text
Triển khai MenuCategory, MenuItem, stockQuantity MVP, FoodOrder, OrderItem và history nếu cần.

Yêu cầu:
- Menu theo Branch.
- Server tính giá.
- Snapshot itemName/unitPrice/quantity/subtotal.
- Session Active bắt buộc.
- Stock lock, không oversell.
- WALLET hoặc PAY_AT_COUNTER theo branch policy.
- State NEW->ACCEPTED->PREPARING->READY->DELIVERED; NEW/ACCEPTED->CANCELLED.
- Cancel có reason, refund và hoàn stock theo policy.
- Staff đúng branch.
- Realtime, notification, audit, idempotency.
- Test money, stock, state, refund, race.
```

---

# PROMPT 13 — BLOCK, REPORT VÀ MODERATION

```text
Triển khai UserBlock, UserReport và ModerationAction nếu cần.

Yêu cầu:
- Block/unblock, không block chính mình.
- Block ảnh hưởng Radar, Invitation và social notification.
- Report có reason/status/context.
- Không lộ reporter cho target.
- Rate limit report.
- Admin moderation theo branch.
- Audit, không hard delete.
- Test filtering, invitation, scope.
```

---

# PROMPT 14 — LFG, INVITATION, LOBBY VÀ CHAT

```text
Triển khai LfgSignal, TeamInvitation, Lobby, LobbyMember, LobbyMessage.

Yêu cầu:
- LFG chỉ khi Session Active và Game Profile hợp lệ.
- Matching theo branch, game, rank, role, zone và block filter.
- Không lộ dữ liệu nhạy cảm.
- Invitation TTL 60 giây, không duplicate pending, rate limit.
- Accept concurrency-safe.
- Lobby có leader, max size theo game, join/leave/kick/transfer/disband.
- Chỉ member xem lobby.
- Chat validation, sanitize, pagination, realtime, retention.
- Session end đóng LFG qua event.
- Notification, audit, migration.
- Test expiry, race, capacity, permission.
```

---

# PROMPT 15 — IOT DEVICE, TELEMETRY VÀ ALERT

```text
Triển khai IotDevice, DeviceTelemetry, DeviceAlert, AlertHistory.

Yêu cầu:
- Device CRUD, serial unique, cùng Branch với Station.
- Capability, heartbeat, telemetry.
- Telemetry API có time range/pagination/limit.
- Mất 3 heartbeat -> OFFLINE.
- Không duplicate open alert.
- Alert OPEN/ACKNOWLEDGED/IN_PROGRESS/RESOLVED/CLOSED.
- Assign Staff, append-only history.
- Critical có thể khóa command cơ khí.
- Không block Session trừ safety.
- Branch scope, notification, audit.
- Test heartbeat, duplicate, transition, scope.
```

---

# PROMPT 16 — SMART STATION COMMAND VÀ MQTT

```text
Triển khai DeviceCommand, CommandHistory, MQTT adapter và apply preference.

Yêu cầu:
- MqttPublisher/MqttSubscriber abstraction.
- Mock adapter dev/test.
- Topic theo branch/station/device.
- Payload có commandId/correlationId/type/value/unit/timestamp.
- Load preference/default, map command, validate safety.
- ACK correlation, timeout, retry tối đa 2.
- Không retry command nguy hiểm mù quáng.
- Duplicate/late ACK idempotent.
- Tổng hợp SUCCESS/PARTIAL_SUCCESS/FAILED/SKIPPED.
- IoT lỗi không block Session.
- Critical -> emergency stop/alert.
- Progress WebSocket, audit, TLS note.
- MQTT Gateway dùng credential riêng.
- Gateway chỉ publish/subscribe đúng topic được cấp.
- Validate branchId/stationId/deviceId theo identity, không chỉ tin payload.
- Reject unknown device.
- Chống ACK giả, telemetry spoofing và topic spoofing cơ bản.
- Duplicate correlationId phải idempotent.
- Test success, partial, timeout, duplicate, critical, fake MQTT message và cross-branch topic.
```

---

# PROMPT 17 — NOTIFICATION DELIVERY VÀ PUSH

```text
Hoàn thiện Notification trên Audit/Event foundation đã có.

Triển khai:
- Notification.
- PushSubscription.
- NotificationDelivery nếu cần.
- EmailSender port.

Yêu cầu:
- In-app, WebSocket, Browser Push foundation, email account security.
- Đúng recipient và đúng branch.
- Mark read/read all, soft-hide delete.
- Subscribe/unsubscribe push.
- Nếu chưa có provider thật thì dùng mock/dev adapter và ghi rõ.
- Delivery status, retry giới hạn, pagination.
- Test recipient, read, realtime, masking.
```

---

# PROMPT 18 — DASHBOARD, REPORT VÀ EXPORT

```text
Triển khai KPI station, occupancy, session, gamer active, session/F&B/top-up revenue, order, alert, device failure, LFG success.

Yêu cầu:
- Aggregate query/projection, không load toàn bộ entity.
- Filter date/week/month/custom/branch/zone/station.
- Timezone rõ; response có from/to/timezone/generatedAt.
- Công thức KPI rõ.
- Refund không cộng sai revenue.
- Branch scope và giới hạn date range.
- Cache ngắn nếu cần.
- CSV bắt buộc; XLSX chỉ khi dependency hợp lý.
- Export đúng filter và audit.
- Test empty, timezone, scope, revenue.
```

---

# PROMPT 19 — BACKGROUND JOBS VÀ RETENTION

```text
Triển khai:
- Expire QR, invitation, LFG.
- Station/device heartbeat timeout.
- Cleanup refresh/reset token.
- Session policy và ending warning.
- Notification/chat retention.
- Retry notification.
- Payment reconciliation nếu adapter hỗ trợ.

Yêu cầu:
- Spring Scheduling.
- Interval, batch size, idempotent.
- Multi-instance lock.
- UTC, metrics, logging.
- Không load toàn bộ bảng.
- Không duplicate alert/event.
- Audit giữ tối thiểu 12 tháng.
- Test replay/idempotency.
```

---

# PROMPT 20 — BACKEND HARDENING VÀ INTEGRATION TEST

```text
Review entity, DTO, controller, transaction, N+1, scope, JWT, IDOR, CORS/CSRF, WebSocket auth, Station credential, concurrency, logging, Flyway, Swagger, Actuator, dependency và test.

Integration flow:
- Auth.
- QR/Session.
- Wallet/Billing.
- Order.
- LFG/Lobby.
- Device/Alert.
- Smart Station.
- Notification.
- Reports.

Coverage:
- Kiểm tra JaCoCo nếu đã có hoặc được phép thêm.
- Service Layer mục tiêu >= 80%.
- Controller Layer mục tiêu >= 75%.
- Business Critical Module mục tiêu >= 85%.
- Không viết test vô nghĩa chỉ để tăng phần trăm.
- Không loại trừ class nghiệp vụ tùy tiện.

Chạy `mvn clean test`.
Không disable test.
Xuất coverage report và danh sách module chưa đạt.
```

---

# PROMPT 21 — FRONTEND FOUNDATION

```text
Chuẩn hóa `client/`.

Yêu cầu:
- React Router.
- Axios instance.
- TanStack Query.
- Chỉ một client state manager.
- Refresh queue tránh nhiều refresh đồng thời và infinite loop.
- Typed API client.
- Auth store.
- Route guard guest/auth/role/permission.
- Layout Gamer/Station/Staff/Admin.
- ErrorBoundary, loading, empty, error, toast, confirm.
- WebSocket abstraction.
- React Hook Form + Zod.
- Env validation.
- Test setup.
- Chạy lint/test/build.
```

---

# PROMPT 22 — FRONTEND AUTH VÀ PROFILE

```text
Triển khai Register, Login, Forgot, Reset, Change Password, Current User, Profile, Game Profile, Station Preference, Session History và Wallet overview.

Yêu cầu:
- Form + Zod.
- Backend error mapping.
- Auth guard và role navigation.
- Không lộ lỗi kỹ thuật.
- Responsive.
- Test form/guard/error.
```

---

# PROMPT 23 — GAMER QR, SESSION VÀ SMART STATION UI

```text
Triển khai QR scanner, manual fallback, station confirm, countdown, expired/used handling, Current Session, end session, Smart Station progress và partial failure UI.

Yêu cầu:
- html5-qrcode.
- Camera permission.
- HTTPS/localhost note.
- WebSocket + polling fallback.
- Backend luôn validate QR.
- Responsive.
- Test camera error/session event/partial.
```

---

# PROMPT 24 — GAMER LFG, LOBBY VÀ CHAT UI

```text
Triển khai Radar filters, Gamer card, block/report, invitation realtime, Lobby, leader actions, chat history/realtime, reconnect và deduplicate event.

Không hiện email/phone/balance.
Backend vẫn kiểm tra quyền.
Có pagination/loading/empty/error.
Test invitation/lobby/chat/reconnect.
```

---

# PROMPT 25 — GAMER MENU, CART, ORDER VÀ WALLET UI

```text
Triển khai Menu, search/filter, Cart, Checkout, Wallet/pay-at-counter, My Orders, realtime status, Wallet Transactions và Top-up khi backend hỗ trợ.

Yêu cầu:
- Backend là nguồn giá cuối.
- Không giả payment success.
- Idempotency-Key.
- Handle out-of-stock và price change.
- Responsive.
- Test cart/checkout/status.
```

---

# PROMPT 26 — STATION KIOSK WEB

```text
Triển khai Station credential, full-screen, QR auto-refresh, network status, heartbeat, WebSocket/polling fallback, Session Started, Current Session, Smart Station progress, Quick Order Panel, auto reset và clear sensitive state.

Không triển khai native overlay.
Test reconnect, credential, QR, session event, reset.
```

---

# PROMPT 27 — STAFF F&B WEB

```text
Triển khai Order Queue, detail, accept/prepare/ready/deliver/cancel/assign, cancel reason, realtime order, sound/browser notification, SLA và pagination.

Chỉ hiện action hợp lệ theo state.
Không thao tác branch khác.
Test state, realtime, scope.
```

---

# PROMPT 28 — STAFF TECHNICAL WEB

```text
Triển khai Device list, Telemetry summary, Alert Queue, filters, acknowledge/assign/start/resolve/close/reopen, Maintenance và realtime alerts.

Yêu cầu state đúng, branch scope và không tải telemetry thô quá lớn.
Test state/filter/realtime.
```

---

# PROMPT 29 — ADMIN MANAGEMENT WEB

```text
Triển khai Branch, Zone, Station, Game/Rank/Role, Menu, User/Staff, Role/Permission, Device, Wallet Adjustment và Audit Log.

Yêu cầu pagination, filter, Branch Admin khác Super Admin, confirm hành động nguy hiểm, không expose secret.
Test CRUD/scope/permission.
```

---

# PROMPT 30 — ADMIN DASHBOARD VÀ REPORT UI

```text
Triển khai KPI cards, Recharts, date/timezone/branch/zone/station filters, report table, export, empty/loading/error và last updated.

Không tự tính KPI trái backend.
Không load raw dataset lớn.
Test filters/empty/export.
```

---

# PROMPT 31 — PWA, NOTIFICATION VÀ RESPONSIVE

```text
Hoàn thiện manifest, service worker, installable app, static cache, offline fallback giới hạn, push subscription, browser permission, notification center/badge và responsive mobile/tablet.

Không cache API nhạy cảm hoặc token.
Service worker versioning.

Browser compatibility:
- Chromium.
- Microsoft Edge hoặc Chromium tương đương.
- WebKit để mô phỏng Safari.
- Mobile Chrome profile.
- Viewport: 375px, tablet, 1366x768, 1920x1080, 2560x1440.
- Ghi rõ browser hoặc thiết bị thật chưa kiểm thử được.

Test PWA/build/responsive/browser matrix.
```

---

# PROMPT 32 — FRONTEND TEST VÀ CLEANUP

```text
Review API types, `any`, duplicate component, route guard, refresh interceptor, WebSocket reconnect, role UI, branch scope, loading/empty/error, accessibility cơ bản, responsive và PWA.

Chạy lint, unit test, build và Playwright flow chính.

Coverage:
- Dùng coverage của Vitest.
- Báo cáo feature quan trọng chưa đạt.
- Không viết test vô nghĩa chỉ để tăng phần trăm.
- Không bỏ qua lỗi.
```

---

# PROMPT 33 — FULLSTACK INTEGRATION VÀ E2E

```text
Chạy toàn hệ thống.

E2E:
- Register/Login/Refresh.
- Admin tạo Branch/Zone/Station.
- Kiosk QR -> Gamer confirm -> Session.
- Smart Station mock MQTT.
- Order -> Staff -> Delivered.
- LFG -> Invitation -> Lobby -> Chat.
- Device timeout -> Alert -> Resolve.
- Notification realtime.
- Dashboard update.

Kiểm tra API contract, CORS, WebSocket, refresh, branch scope, timezone, idempotency và error UI.
```

---

# PROMPT 34 — DOCKER, NGINX VÀ CI/CD

```text
Triển khai Dockerfile backend, frontend build, Nginx và Docker Compose gồm PostgreSQL, Redis, MQTT Broker, Backend, Frontend/Nginx.

Yêu cầu:
- Healthcheck, volumes, network, env, `.env.example`.
- Profile local/test/prod.
- `/api` proxy, `/ws` WebSocket proxy, SPA fallback.
- GitHub Actions backend/frontend/build.
- Không secret thật, không seed password production.
- Chạy `docker compose up --build` và kiểm tra health/Swagger/frontend/ws/redis/mqtt.
```

---

# PROMPT 35 — DOCUMENTATION, GIT WORKFLOW VÀ HANDOVER

```text
Bạn là Technical Writer, QA Lead và Release Manager.

Hoàn thiện và kiểm tra:
- README.md.
- Giữ nguyên và liên kết:
  - `md/SRS_NEXUS_WEB_FULLSTACK.md`
  - `md/NEXUS_Project_Scope_Chi_Tiet.md`
- System-Architecture.md.
- System-Flow.md.
- Database-Design.md.
- API-Documentation.md.
- Requirement-Traceability-Matrix.md.
- Deployment-Guide.md.
- Disaster-Recovery-Guide.md.
- Monitoring-Guide.md.
- Postman-Guide.md và Postman Collection.
- Test-Plan.md.
- Test-Report.md.
- Security-Test-Report.md.
- Performance-Test-Report.md.
- UAT-Plan.md.
- UAT-Test-Cases.md.
- UAT-Report.md.
- Error-Log.md.
- Changelog.md.
- Demo-Script.md.
- Prompt-Chain.md.
- CONTRIBUTING.md.

README phải có:
- Prerequisites.
- Ports.
- Environment variables.
- Cách chạy local.
- Cách chạy Docker.
- Cách test.
- Demo accounts.
- Troubleshooting.
- Liên kết đến toàn bộ tài liệu.

CONTRIBUTING.md phải có:
- Branch strategy:
  main, develop, feature/*, fix/*, hotfix/*, release/*.
- Commit convention.
- Pull Request template.
- Review checklist.
- Không push trực tiếp main.
- Không commit `.env`, secret hoặc build output.

Yêu cầu:
- Không tạo hai bản SRS hoặc Scope mâu thuẫn.
- Không ghi đè tài liệu nguồn nếu chưa cần.
- Ghi rõ integration thật, mock, limitation và known risks.
- Error Log chỉ ghi lỗi thực tế, không ghi lỗi giả lập.
- Không đưa token, password hoặc secret vào tài liệu.
```

---


# PROMPT 36 — VOICE PROVIDER ADAPTER VÀ LOBBY VOICE

```text
Bạn là Senior Realtime Communication Engineer.

Mục tiêu:
Triển khai Voice Chat foundation cho Lobby theo hướng adapter, không tự phát triển Voice Codec.

Yêu cầu:
1. Tạo `VoiceProviderPort`.
2. Tạo Development Mock Voice Provider.
3. Không tự chọn provider thật nếu chưa có cấu hình.
4. Khi Lobby bật Voice:
   - Tạo Voice Channel.
   - Gắn channel với Lobby.
   - Sinh Voice Token ngắn hạn cho từng member.
5. Voice Token:
   - Gắn User ID.
   - Gắn Lobby ID.
   - Có TTL ngắn.
   - Không dùng cho Lobby khác.
6. Chỉ Lobby Member được xin token.
7. User rời Lobby không được xin token mới.
8. Lobby đóng thì đóng hoặc vô hiệu Voice Channel nếu provider hỗ trợ.
9. Provider lỗi:
   - Không rollback Lobby.
   - Text Chat vẫn hoạt động.
   - Trả trạng thái `VOICE_UNAVAILABLE`.
10. Webhook:
   - Verify signature.
   - Anti-replay.
   - Idempotent.
   - Không log secret.
11. Circuit breaker/fallback chỉ dùng nếu dependency hiện có hỗ trợ.
12. Thêm `.env.example`:
   - VOICE_PROVIDER_KEY
   - VOICE_WEBHOOK_SECRET
   - VITE_ENABLE_VOICE
13. Frontend chỉ hiện Voice UI khi feature flag bật.
14. Ghi rõ mock hay provider thật.

Test:
- Non-member bị từ chối.
- Token đúng User/Lobby.
- Token hết hạn.
- Provider lỗi nhưng Lobby vẫn hoạt động.
- Webhook chữ ký sai.
- Webhook replay.
- Lobby đóng xử lý Voice Channel.
```

---

# PROMPT 37 — SECURITY TEST VÀ OWASP REVIEW

```text
Bạn là Senior Application Security Engineer.

Không thêm nghiệp vụ mới.

Kiểm tra bắt buộc:
- Login brute force.
- Account enumeration.
- JWT tampering.
- Expired token.
- Token replay.
- Refresh Token reuse.
- Logout token reuse.
- Role bypass.
- Permission bypass.
- Branch scope bypass.
- IDOR trên UUID.
- Mass assignment.
- SQL Injection.
- Stored/Reflected XSS.
- CORS sai origin.
- CSRF theo chiến lược hiện tại.
- Invalid WebSocket authentication.
- Unauthorized STOMP subscription.
- Fake Station Credential.
- Fake MQTT message.
- MQTT topic spoofing.
- Fake Payment Webhook.
- Payment replay.
- Voice Webhook replay nếu Voice bật.
- Rate-limit bypass.
- Sensitive data exposure.
- Stack trace exposure.
- Secret trong source.
- Dependency vulnerability scan.
- Actuator exposure.
- Swagger exposure production.
- File/path traversal nếu có upload.

MFA:
- Nếu chưa thuộc MVP bắt buộc, ghi accepted risk.
- Không triển khai MFA nửa vời.
- Nếu triển khai phải có recovery flow và test.

Đầu ra:
- Security-Test-Report.md.
- Severity Critical/High/Medium/Low.
- Evidence và reproduction.
- Fix tận gốc.
- Regression test sau fix.
- Không đánh dấu PASS nếu chỉ review bằng mắt.
```

---

# PROMPT 38 — PERFORMANCE, LOAD VÀ CONCURRENCY TEST

```text
Bạn là Senior Performance Engineer.

Ngưỡng cần kiểm chứng:
- P95 REST API thông thường dưới 500ms.
- QR Login dưới 5 giây.
- Notification realtime dưới 3 giây.
- Smart Station orchestration dưới 10 giây.
- Dashboard không tải toàn bộ dữ liệu thô.

Kịch bản:
- Login.
- Refresh Token.
- QR create/confirm.
- Radar/LFG query.
- Invitation.
- Order creation.
- Staff order queue.
- Dashboard overview.
- Telemetry ingestion.
- WebSocket concurrent connections.

Đo:
- P50/P95/P99.
- Throughput.
- Error rate.
- DB connection.
- Redis latency.
- CPU/RAM.

Concurrency:
- QR Confirm.
- Active Session create.
- Wallet debit.
- Stock update.
- Invitation accept.
- Payment callback.

Yêu cầu:
- Ưu tiên k6 hoặc công cụ nhẹ nếu chưa có.
- Tạo dữ liệu test tái lập.
- Tách dữ liệu performance khỏi development data.
- Không tuyên bố đạt 10.000 user nếu môi trường không đủ.
- Phát hiện N+1, slow query, missing index.
- Tạo Performance-Test-Report.md.
```

---

# PROMPT 39 — OBSERVABILITY, METRICS VÀ OPERATIONAL ALERTING

```text
Bạn là Senior Site Reliability Engineer.

Triển khai:
- Actuator health/info/metrics.
- Liveness/readiness.
- Không expose endpoint nhạy cảm công khai.
- Correlation ID xuyên REST, WebSocket, Domain Event và MQTT.
- Structured logging.
- Centralized logging bằng Loki hoặc ELK nếu stack đã chọn.
- Prometheus metrics nếu phù hợp.
- Grafana dashboard nếu có Prometheus.

Business metrics:
- Login success/failure.
- Rate-limit rejection.
- Active Session.
- QR confirm duration.
- Order creation/status duration.
- Wallet mutation failure.
- Active WebSocket connection.
- MQTT reconnect.
- Device heartbeat timeout.
- Open Critical Alert.
- Notification latency.
- Dashboard query duration.
- External provider error.

Alert:
- Backend unhealthy.
- Database failure.
- Redis unavailable.
- MQTT disconnected.
- Critical device alert.
- Payment webhook failure.
- Error rate vượt ngưỡng.
- P95 latency vượt SLA.

Ràng buộc:
- Không log token/password/secret.
- Không dùng email/userId làm metric label nếu gây high cardinality.
- Tạo Monitoring-Guide.md.
- Ghi rõ phần nào chạy thật và phần nào là cấu hình mẫu.
```

---

# PROMPT 40 — BACKUP, RESTORE VÀ DISASTER RECOVERY

```text
Bạn là Senior Database Reliability Engineer.

Mục tiêu:
Kiểm thử RPO <= 15 phút và RTO <= 4 giờ.

Thực hiện:
- Backup PostgreSQL.
- Backup MQTT config.
- Backup uploaded assets nếu có.
- Redis không được xem là nguồn dữ liệu chính.
- Tạo backup script.
- Tạo restore script.
- Timestamp UTC.
- Không hardcode password.
- Retention policy.
- Integrity check.
- Restore vào database mới.
- Chạy Flyway validate sau restore.
- Kiểm tra users, sessions, wallets, wallet_transactions, orders, audit_logs.
- Đo thời gian backup/restore thực tế.
- Không tuyên bố đạt RPO/RTO nếu chưa test.
- Tạo Disaster-Recovery-Guide.md.
```

---

# PROMPT 41 — UAT VÀ DEMO ACCEPTANCE

```text
Bạn là QA Lead và Business Analyst.

Tạo và chạy UAT cho:
- Gamer.
- Staff F&B.
- Staff Technical.
- Branch Admin.
- Super Admin.
- Station Kiosk.

Mỗi test case:
- UAT ID.
- Requirement ID.
- Actor.
- Preconditions.
- Test data.
- Steps.
- Expected result.
- Actual result.
- Evidence.
- PASS/FAIL/BLOCKED.
- Tester note.

Flow:
- Register/Login.
- QR Login.
- Session.
- Smart Station.
- LFG/Invitation/Lobby.
- Voice fallback nếu bật.
- Order/Wallet.
- Staff Order.
- Device/Alert.
- Notification.
- Dashboard.
- Export.
- Branch Scope.

Tạo:
- UAT-Plan.md.
- UAT-Test-Cases.md.
- UAT-Report.md.
- Demo-Script.md.

Không đánh dấu PASS khi chưa có actual result và evidence.
```

---

# PROMPT 42 — FINAL SRS TRACEABILITY AUDIT


```text
Đối chiếu SRS, Scope, backend, frontend, database, Swagger, Postman, test, security report, performance report, UAT, backup/restore, observability, Docker và documentation.

Tạo RTM:
| Requirement | Backend | Frontend | Database | Test | Status | Evidence | Gap |

Status: PASS/PARTIAL/FAIL/OUT_OF_SCOPE/NOT_VERIFIED.

Không sửa trước khi báo cáo.
Xếp gap Critical/High/Medium/Low.
Sau đó sửa theo thứ tự, chạy lại toàn bộ build/test/E2E và cập nhật RTM.
Không tuyên bố PASS khi chưa có evidence.
```

---

# CHECKPOINT BẮT BUỘC

## Checkpoint A — sau Prompt 04
Kiểm tra foundation, Flyway, Security, Redis, rate limit, idempotency và lock. Không thêm module mới.

## Checkpoint B — sau Prompt 12
Chạy Auth -> QR -> Session -> Wallet -> Order -> Staff -> Refund. Không tiếp tục nếu flow lỗi.

## Checkpoint C — sau Prompt 16
Chạy Session -> LFG -> Lobby -> Chat và Device -> Alert -> Smart Station -> MQTT ACK.

# QUY TẮC DỪNG

Không chuyển bước nếu:
- Compile/build fail.
- Test fail chưa rõ nguyên nhân.
- Flyway fail.
- Có entity/enum/service trùng.
- Có branch scope leak.
- Có race đã biết ở QR, Session, Wallet, Payment, Stock hoặc Invitation.
- Có secret trong source.
- Có security bypass Critical.

# DEFINITION OF DONE

Một module chỉ hoàn thành khi:
- Đúng SRS/Scope.
- Schema và migration đúng.
- DTO không lộ dữ liệu nhạy cảm.
- API, validation, quyền và branch scope đúng.
- Business rule ở service.
- Transaction/concurrency đã kiểm tra.
- Audit/event tích hợp khi cần.
- Test quan trọng có đủ.
- Swagger cập nhật.
- Build/test pass.
- Không còn lỗi Critical.
- Có ghi rủi ro còn lại.


# CHECKPOINT D — GO-LIVE READINESS

Sau Prompt 41, bắt buộc kiểm tra:
- Security Test không còn Critical.
- Performance Test có kết quả và giới hạn rõ.
- Backup/Restore đã thử thật.
- Monitoring/Health hoạt động.
- UAT có evidence.
- Browser matrix có kết quả.
- Coverage report đã tạo.
- Không còn branch scope leak.
- Không còn race condition đã biết ở nghiệp vụ trọng yếu.

Không chạy Final Audit nếu Checkpoint D chưa đạt.
