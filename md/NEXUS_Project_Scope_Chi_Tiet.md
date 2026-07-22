# NEXUS SMART CYBER ESPORTS
## Project Scope & Technical Specification

> **Document Code:** NEXUS-PROJECT-SCOPE-01  
> **Version:** 3.0  
> **Project Type:** Fullstack Web Application + IoT Integration  
> **Architecture:** ReactJS Web Client + Spring Boot REST API + WebSocket + MQTT  
> **Development Timeline:** 07/07/2026 - 17/07/2026  
> **Primary Platform:** Responsive Web / PWA  
> **Target Environment:** Cyber Gaming Center / Esports Center  

---

# 1. Project Overview

## 1.1 Project Name

**NEXUS Smart Cyber Esports**

## 1.2 Project Description

NEXUS Smart Cyber Esports là hệ thống quản lý và vận hành phòng máy esports thông minh theo mô hình Web Application.

Hệ thống không chỉ quản lý tài khoản và phiên sử dụng máy mà còn tích hợp các chức năng:

- Đăng nhập máy trạm bằng QR.
- Quản lý hồ sơ Gamer.
- Cá nhân hóa không gian chơi thông qua Smart Station.
- Tìm người chơi và tạo đội tại phòng máy.
- Quản lý lời mời và Lobby.
- Đặt đồ ăn, thức uống ngay tại máy.
- Quản lý đơn hàng dành cho Staff.
- Theo dõi trạng thái máy trạm và thiết bị IoT.
- Quản lý cảnh báo kỹ thuật.
- Dashboard và báo cáo vận hành dành cho Admin.
- Ghi nhận Audit Log cho các thao tác quan trọng.

Phiên bản này được định hướng là **Web-only**, không phát triển Mobile App native và không phát triển Desktop Client native.

Toàn bộ người dùng truy cập hệ thống bằng trình duyệt:

- Gamer sử dụng Web Responsive hoặc PWA trên điện thoại.
- Máy trạm sử dụng Station Web ở chế độ Kiosk.
- Staff sử dụng Staff Web.
- Admin sử dụng Admin Web.

## 1.3 Business Problem

Các phòng máy truyền thống thường gặp những vấn đề:

- Đăng nhập và mở máy còn phụ thuộc nhân viên.
- Cấu hình bàn, ghế, đèn và thiết bị chưa được cá nhân hóa.
- Người chơi khó tìm đồng đội đang có mặt cùng chi nhánh.
- Người chơi phải rời máy để gọi món.
- Staff xử lý đơn bằng trao đổi thủ công.
- Không theo dõi tập trung tình trạng thiết bị.
- Không có cảnh báo lỗi theo thời gian thực.
- Quản lý thiếu dashboard tổng hợp và dữ liệu vận hành.
- Khó truy vết các thao tác quản trị.

## 1.4 Project Objectives

Mục tiêu của dự án:

- Xây dựng hệ thống quản lý phòng máy esports trên nền tảng Web.
- Tạo REST API theo chuẩn Spring Boot.
- Xây dựng giao diện ReactJS responsive.
- Áp dụng JWT Authentication và Refresh Token.
- Phân quyền Gamer, Staff và Admin.
- Hỗ trợ QR Login tại máy trạm.
- Hỗ trợ thông báo thời gian thực bằng WebSocket.
- Tích hợp IoT thông qua MQTT.
- Quản lý phiên chơi và trạng thái máy trạm.
- Quản lý đặt món và quy trình xử lý đơn.
- Hỗ trợ Local Matchmaking và Lobby.
- Xây dựng Admin Dashboard trực quan.
- Ghi Audit Log cho thao tác quan trọng.
- Thiết kế kiến trúc dễ bảo trì và mở rộng.

## 1.5 Success Metrics

- QR Login hoàn thành trong tối đa 5 giây ở điều kiện bình thường.
- Thông báo đơn hàng và lời mời được gửi trong tối đa 3 giây.
- Lệnh Smart Station hoàn thành trong tối đa 10 giây.
- Không tạo trùng Play Session khi request bị gửi lại.
- Mỗi Gamer chỉ có tối đa một phiên Active.
- Mọi thay đổi trạng thái đơn phải tuân theo state machine.
- Cảnh báo thiết bị được tạo sau khi mất quá số heartbeat quy định.
- Gamer không nhìn thấy email, số điện thoại hoặc số dư của người chơi khác.
- Staff và Admin chỉ truy cập dữ liệu đúng phạm vi chi nhánh.

---

# 2. User Roles

## 2.1 Gamer

Gamer là khách hàng sử dụng dịch vụ tại phòng máy.

Quyền chính:

- Đăng ký tài khoản.
- Đăng nhập hệ thống.
- Quét QR để đăng nhập máy trạm.
- Xem và cập nhật hồ sơ.
- Quản lý cấu hình Smart Station.
- Quản lý Game Profile.
- Xem phiên chơi hiện tại.
- Xem lịch sử phiên chơi.
- Xem số dư.
- Tìm người chơi.
- Phát tín hiệu LFG.
- Gửi và phản hồi lời mời.
- Tham gia Lobby.
- Đặt món.
- Theo dõi trạng thái đơn.
- Nhận thông báo.
- Chặn hoặc report người chơi khác.

## 2.2 Staff F&B

Staff F&B là nhân viên phụ trách đồ ăn và thức uống.

Quyền chính:

- Đăng nhập Staff Web.
- Xem hàng đợi đơn thuộc chi nhánh.
- Xem chi tiết đơn.
- Nhận đơn.
- Chuyển trạng thái đơn.
- Ghi chú xử lý.
- Hủy đơn có lý do.
- Chuyển đơn cho Staff khác.
- Xem lịch sử đơn.
- Xem menu và tồn kho.
- Cập nhật trạng thái món nếu được cấp quyền.

## 2.3 Staff Technical

Staff Technical là nhân viên phụ trách máy trạm và thiết bị IoT.

Quyền chính:

- Xem danh sách máy trạm.
- Xem trạng thái thiết bị.
- Xem cảnh báo kỹ thuật.
- Xác nhận cảnh báo.
- Ghi chú quá trình xử lý.
- Chuyển thiết bị sang Maintenance.
- Đóng cảnh báo.
- Xem lịch sử downtime.
- Theo dõi heartbeat và telemetry.

## 2.4 Branch Admin

Branch Admin quản lý một chi nhánh.

Quyền chính:

- Quản lý Gamer và Staff thuộc chi nhánh.
- Quản lý Zone và Station.
- Quản lý thiết bị IoT.
- Quản lý menu.
- Quản lý giá.
- Quản lý cấu hình chi nhánh.
- Xem Dashboard.
- Xem báo cáo doanh thu.
- Xem báo cáo vận hành.
- Xem cảnh báo.
- Xem Audit Log trong phạm vi chi nhánh.
- Xuất báo cáo.

## 2.5 Super Admin

Super Admin quản trị toàn hệ thống.

Quyền chính:

- Quản lý tất cả chi nhánh.
- Quản lý tài khoản Admin.
- Quản lý vai trò và quyền.
- Quản lý cấu hình hệ thống.
- Quản lý tích hợp bên ngoài.
- Xem dữ liệu tổng hợp toàn hệ thống.
- Xem Audit Log toàn hệ thống.
- Khóa hoặc mở khóa tài khoản.
- Quản lý danh mục dùng chung.

---

# 3. Project Scope

Trong phạm vi phiên bản hiện tại, hệ thống triển khai các module sau.

---

# 3.1 Authentication & Authorization

## Features

- Register Gamer.
- Login bằng email hoặc số điện thoại.
- JWT Access Token.
- Refresh Token.
- Refresh Token Rotation.
- Logout.
- Revoke Token.
- Forgot Password.
- Reset Password.
- Change Password.
- Current User.
- Role-Based Access Control.
- Branch-Based Data Scope.
- Account Lock.
- Account Activation.
- Rate Limit cho API xác thực.

## Business Rules

- Email phải duy nhất.
- Số điện thoại phải duy nhất nếu được cung cấp.
- Mật khẩu phải đạt yêu cầu bảo mật.
- Access Token có thời hạn ngắn.
- Refresh Token có thời hạn dài hơn.
- Refresh Token cũ bị thu hồi sau khi rotation.
- Tài khoản Inactive hoặc Locked không được đăng nhập.
- Admin có thể khóa tài khoản.
- Gamer không thể truy cập API của Staff hoặc Admin.

---

# 3.2 QR Station Login

## Description

Máy trạm hiển thị mã QR. Gamer sử dụng Web/PWA trên điện thoại để quét và xác nhận đăng nhập.

## Features

- Station Web tạo QR Session.
- QR chứa stationId, nonce và thời hạn.
- QR tự động làm mới khi hết hạn.
- Gamer quét QR bằng camera trình duyệt.
- Gamer xác nhận đăng nhập máy trạm.
- Backend kiểm tra tài khoản.
- Backend kiểm tra Station.
- Backend kiểm tra số dư hoặc điều kiện sử dụng.
- Backend tạo Play Session.
- Station Web nhận Session Token qua WebSocket hoặc polling.
- Station Web chuyển sang màn hình phiên chơi.
- Hỗ trợ retry idempotent.
- Admin có thể đăng nhập dự phòng bằng tài khoản và mật khẩu.

## Business Rules

- QR chỉ sử dụng một lần.
- QR hết hạn sau tối đa 60 giây.
- Station chỉ có một Play Session Active.
- Gamer chỉ có một Play Session Active.
- QR hết hạn không được xác nhận.
- QR của Station đang bận không được sử dụng.
- Retry không được tạo trùng Session.

---

# 3.3 Gamer Profile

## Features

- Xem thông tin cá nhân.
- Cập nhật tên hiển thị.
- Cập nhật avatar.
- Cập nhật ngày sinh nếu cần.
- Cập nhật chiều cao.
- Cập nhật cân nặng.
- Cập nhật chế độ Night Mode.
- Xem lịch sử hoạt động.
- Xem lịch sử phiên chơi.
- Xem số dư tài khoản.
- Đổi mật khẩu.

## Validation

- Tên hiển thị không được để trống.
- Avatar phải đúng định dạng.
- Chiều cao phải nằm trong giới hạn hợp lệ.
- Cân nặng phải nằm trong giới hạn hợp lệ.
- Gamer chỉ chỉnh sửa hồ sơ của chính mình.

---

# 3.4 Game Profile

## Features

Gamer có thể tạo hồ sơ riêng cho từng game.

Thông tin gồm:

- Game.
- In-game name.
- Rank.
- Tier.
- Preferred role.
- Secondary role.
- Play style.
- Mô tả ngắn.
- Trạng thái hiển thị trên Radar.

## Business Rules

- Một Gamer chỉ có một profile cho mỗi game.
- Rank phải thuộc danh mục rank của game.
- Role phải thuộc danh mục role của game.
- Game Profile phải hợp lệ trước khi phát LFG.

---

# 3.5 Smart Station Preference

## Features

- Thiết lập chiều cao bàn.
- Thiết lập góc ghế.
- Thiết lập màu RGB.
- Thiết lập độ sáng.
- Thiết lập mouse DPI.
- Thiết lập Night Mode.
- Khôi phục cấu hình mặc định.
- Xem cấu hình hiện tại.
- Lưu cấu hình cho lần đăng nhập tiếp theo.
- Áp dụng cấu hình khi bắt đầu Session.

## Business Rules

- Desk Height: 60 - 120 cm.
- Chair Angle: 90 - 145 độ.
- RGB Color: định dạng `#RRGGBB`.
- Mouse DPI phải thuộc phạm vi thiết bị hỗ trợ.
- Lỗi IoT không được ngăn Gamer sử dụng máy.
- Thiết bị cơ khí phải có hard limit tại firmware.
- Lệnh Critical phải dừng thiết bị.
- Lệnh timeout được retry tối đa 2 lần.

---

# 3.6 Play Session Management

## Features

- Tạo phiên khi QR Login thành công.
- Lấy phiên hiện tại.
- Theo dõi thời gian bắt đầu.
- Theo dõi thời gian sử dụng.
- Tính chi phí tạm tính.
- Kết thúc phiên.
- Kết thúc phiên bởi Staff/Admin.
- Xem lịch sử phiên.
- Xem Station đang sử dụng.
- Cập nhật heartbeat của Station Web.
- Tự động đóng phiên khi hết điều kiện sử dụng.
- Không tạo phiên trùng.

## Session Status

- PENDING.
- ACTIVE.
- PAUSED.
- COMPLETED.
- CANCELLED.
- TERMINATED.

## Business Rules

- Một Gamer chỉ có một phiên Active.
- Một Station chỉ có một phiên Active.
- Chỉ phiên Active mới được dùng Radar và đặt món tại máy.
- Kết thúc Session phải đóng LFG đang hoạt động.
- Kết thúc Session phải cập nhật trạng thái Station.
- Session phải ghi lại startTime, endTime và totalCost.

---

# 3.7 Local Matchmaking / LFG

## Description

Gamer đang chơi tại phòng máy có thể tìm người chơi khác theo game, rank, role và chi nhánh.

## Features

- Tạo LFG Signal.
- Chọn game.
- Chọn rank.
- Chọn role.
- Nhập lời nhắn.
- Xem danh sách Gamer phù hợp.
- Lọc theo chi nhánh.
- Lọc theo Zone.
- Lọc theo rank.
- Lọc theo role.
- Gửi lời mời.
- Hủy LFG.
- Gia hạn LFG.
- Tự động hết hạn.
- Ẩn người chơi bị block.
- Report người chơi.

## Matching Priority

1. Cùng chi nhánh.
2. Cùng game.
3. Rank nằm trong độ lệch cho phép.
4. Role bổ sung.
5. Cùng Zone hoặc gần Station.
6. Gamer đang có Session Active.

## Business Rules

- Chỉ Gamer có Session Active mới xuất hiện.
- LFG hết hạn sau 15 phút.
- Không hiển thị email.
- Không hiển thị số điện thoại.
- Không hiển thị số dư.
- Người chơi đã block nhau không nhìn thấy nhau.
- Session kết thúc thì LFG tự đóng.

---

# 3.8 Team Invitation

## Features

- Gửi lời mời.
- Nhận notification.
- Xem chi tiết lời mời.
- Accept.
- Reject.
- Cancel.
- Auto Expire.
- Không gửi spam.
- Kiểm tra trạng thái người gửi và người nhận.

## Invitation Status

- PENDING.
- ACCEPTED.
- REJECTED.
- EXPIRED.
- CANCELLED.

## Business Rules

- Lời mời hết hạn sau 60 giây.
- Hai Gamer phải đang Active.
- Không gửi lời mời cho người bị block.
- Không tạo nhiều lời mời Pending giống nhau.
- Accept thành công sẽ tạo hoặc cập nhật Lobby.

---

# 3.9 Lobby & Text Chat

## Features

- Tạo Lobby.
- Thêm thành viên.
- Rời Lobby.
- Xóa thành viên.
- Chuyển Leader.
- Giải tán Lobby.
- Text Chat.
- Xem thành viên.
- Trạng thái Online.
- Giới hạn thành viên theo game.
- Tạo Voice Channel qua provider nếu được cấu hình.

## Lobby Status

- OPEN.
- READY.
- IN_GAME.
- CLOSED.

## Business Rules

- Chỉ thành viên Lobby được xem dữ liệu Lobby.
- Chỉ Leader được mời hoặc xóa thành viên.
- Lobby không vượt quá giới hạn game.
- Voice lỗi không được làm hỏng Lobby text.
- Voice Token có thời hạn ngắn.
- Voice Token gắn với Lobby và User.

---

# 3.10 Food & Beverage Menu

## Features

- Xem danh mục món.
- Xem danh sách món.
- Tìm kiếm món.
- Lọc theo danh mục.
- Xem giá.
- Xem hình ảnh.
- Xem trạng thái còn hàng.
- Xem thời gian chuẩn bị dự kiến.
- Thêm vào giỏ hàng.
- Cập nhật số lượng.
- Xóa khỏi giỏ hàng.
- Ghi chú món.
- Tính tổng tiền.

## Menu Item Status

- ACTIVE.
- INACTIVE.
- OUT_OF_STOCK.

## Business Rules

- Không đặt món Inactive.
- Không đặt món OutOfStock.
- Giá được lấy theo chi nhánh.
- Giá phải được snapshot khi tạo Order.
- Số lượng phải lớn hơn 0.
- Mỗi món có thể có giới hạn số lượng.

---

# 3.11 Gamer Food Ordering

## Features

- Tạo đơn hàng.
- Chọn phương thức thanh toán.
- Thanh toán bằng số dư.
- Thanh toán tại quầy nếu chi nhánh cho phép.
- Xem trạng thái đơn.
- Xem lịch sử đơn.
- Hủy đơn nếu còn trạng thái cho phép.
- Nhận thông báo khi đơn thay đổi.
- Đặt món từ Station Web.
- Đặt món từ Gamer Web/PWA.

## Order Status

- NEW.
- ACCEPTED.
- PREPARING.
- READY.
- DELIVERED.
- CANCELLED.

## Business Rules

- Order phải thuộc Session hoặc Station hợp lệ.
- Giá được chốt tại lúc tạo đơn.
- Tồn kho được kiểm tra trước khi tạo.
- Không đủ số dư thì không trừ tiền.
- Hủy đơn đã thanh toán phải thực hiện hoàn tiền.
- Không được chuyển trạng thái sai chuỗi.

## State Machine

```text
NEW -> ACCEPTED -> PREPARING -> READY -> DELIVERED

NEW -> CANCELLED
ACCEPTED -> CANCELLED
```

---

# 3.12 Staff Order Management

## Features

- Xem hàng đợi đơn mới.
- Lọc theo trạng thái.
- Lọc theo thời gian.
- Xem Station nhận hàng.
- Xem chi tiết món.
- Accept Order.
- Chuyển Preparing.
- Chuyển Ready.
- Chuyển Delivered.
- Cancel Order.
- Ghi lý do hủy.
- Chuyển nhân viên xử lý.
- Nhận âm báo đơn mới.
- Xem SLA đơn.
- Xem lịch sử cập nhật.

## Business Rules

- Staff chỉ xem đơn thuộc chi nhánh.
- Staff phải đăng nhập.
- Mọi thay đổi trạng thái phải ghi người thực hiện.
- Cancel phải có lý do.
- Delivered phải có thời gian giao.
- Không được bỏ qua bước trạng thái.

---

# 3.13 Branch, Zone & Station Management

## Features

### Branch

- Tạo chi nhánh.
- Cập nhật chi nhánh.
- Kích hoạt hoặc vô hiệu hóa.
- Cấu hình múi giờ.
- Cấu hình chính sách thanh toán.
- Cấu hình thời gian hoạt động.

### Zone

- Tạo Zone.
- Cập nhật Zone.
- Gán Zone vào Branch.
- Quản lý loại Zone.
- Xem số Station trong Zone.

### Station

- Tạo Station.
- Cập nhật Station.
- Gán Zone.
- Cấu hình Station Code.
- Xem trạng thái.
- Khóa Station.
- Chuyển Maintenance.
- Xem Session hiện tại.
- Xem thiết bị gắn với Station.

## Station Status

- AVAILABLE.
- OCCUPIED.
- OFFLINE.
- MAINTENANCE.
- DISABLED.

---

# 3.14 IoT Device Management

## Device Types

- Smart Desk.
- Gaming Chair.
- RGB Lighting.
- Mouse.
- Keyboard.
- Headset.
- Environmental Sensor.
- IoT Gateway.

## Features

- Đăng ký thiết bị.
- Gán thiết bị vào Station.
- Cập nhật serial number.
- Cập nhật firmware version.
- Theo dõi trạng thái.
- Nhận heartbeat.
- Nhận telemetry.
- Gửi command.
- Xem command history.
- Chuyển Maintenance.
- Disable thiết bị.
- Xem thời gian hoạt động.
- Xem lần kết nối cuối.

## Device Status

- ONLINE.
- DEGRADED.
- OFFLINE.
- MAINTENANCE.
- DISABLED.

## Business Rules

- Serial Number phải duy nhất.
- Device phải thuộc một Branch.
- Device gắn Station phải đúng Branch.
- Device Offline sau 3 heartbeat bị bỏ lỡ.
- Lệnh nguy hiểm phải được validate.
- Command phải có correlationId.
- Command phải ghi log.
- Thiết bị cơ khí phải có hard limit độc lập.

---

# 3.15 Device Alert Management

## Features

- Tạo cảnh báo tự động.
- Tạo cảnh báo thủ công.
- Phân loại severity.
- Gửi notification Staff Technical.
- Acknowledge Alert.
- Assign Staff.
- Ghi chú xử lý.
- Resolve Alert.
- Close Alert.
- Reopen Alert.
- Xem lịch sử.
- Lọc theo Branch, Station, Device và Severity.

## Alert Severity

- INFO.
- WARNING.
- HIGH.
- CRITICAL.

## Alert Status

- OPEN.
- ACKNOWLEDGED.
- IN_PROGRESS.
- RESOLVED.
- CLOSED.

## Business Rules

- Critical Alert có thể khóa điều khiển cơ khí.
- Chỉ Staff Technical hoặc Admin được xử lý.
- Mọi cập nhật phải lưu thời gian và người thực hiện.
- Alert không được xóa cứng.

---

# 3.16 Notification

## Channels

- In-app Notification.
- WebSocket Notification.
- Browser Notification.
- Web Push nếu PWA hỗ trợ.
- Email chỉ dùng cho tác vụ tài khoản nếu được cấu hình.

## Notification Types

- QR Login Success.
- Session Started.
- Session Ending.
- Smart Station Success.
- Smart Station Partial Failure.
- Team Invitation.
- Lobby Update.
- New Order.
- Order Accepted.
- Order Ready.
- Order Delivered.
- Device Alert.
- Account Security Alert.

## Features

- Xem danh sách thông báo.
- Đánh dấu đã đọc.
- Đánh dấu tất cả đã đọc.
- Xóa thông báo cá nhân.
- Nhận realtime.
- Điều hướng đến màn hình liên quan.

---

# 3.17 Wallet & Transaction

## Features

- Xem số dư.
- Xem lịch sử giao dịch.
- Nạp tiền qua Payment Gateway.
- Trừ tiền giờ chơi.
- Trừ tiền đơn hàng.
- Hoàn tiền.
- Admin điều chỉnh có lý do.
- Lọc giao dịch theo loại và thời gian.

## Transaction Types

- TOP_UP.
- SESSION_CHARGE.
- ORDER_PAYMENT.
- REFUND.
- ADMIN_ADJUSTMENT.

## Business Rules

- Số dư không được âm.
- Mọi thay đổi số dư phải tạo transaction.
- Không cập nhật số dư trực tiếp ngoài Wallet Service.
- Payment Callback phải kiểm tra chữ ký.
- Callback phải chống replay.
- Refund phải tham chiếu giao dịch gốc.

---

# 3.18 Admin Dashboard

## KPIs

- Tổng Station.
- Station Active.
- Station Available.
- Station Offline.
- Occupancy Rate.
- Tổng Session.
- Tổng Gamer Active.
- Doanh thu giờ chơi.
- Doanh thu F&B.
- Doanh thu nạp tiền.
- Số đơn mới.
- Thời gian xử lý đơn trung bình.
- Số Alert.
- Tỷ lệ thiết bị lỗi.
- Tỷ lệ LFG thành công.

## Charts

- Line Chart doanh thu theo thời gian.
- Bar Chart doanh thu theo chi nhánh.
- Pie Chart Station Status.
- Pie Chart Order Status.
- Heatmap theo Zone.
- Line Chart số Session.
- Bar Chart lỗi theo Device Type.

## Filters

- Theo ngày.
- Theo tuần.
- Theo tháng.
- Khoảng thời gian tùy chọn.
- Theo Branch.
- Theo Zone.
- Theo Station.

---

# 3.19 Reports

## Features

- Báo cáo doanh thu.
- Báo cáo Session.
- Báo cáo F&B.
- Báo cáo Station.
- Báo cáo IoT.
- Báo cáo Alert.
- Báo cáo User.
- Báo cáo Matchmaking.
- Export CSV.
- Export XLSX.
- Hiển thị thời điểm dữ liệu cập nhật gần nhất.

## Business Rules

- Branch Admin chỉ xem chi nhánh của mình.
- Super Admin được xem nhiều chi nhánh.
- File export phải tuân theo bộ lọc.
- Dữ liệu nhạy cảm phải được ẩn.

---

# 3.20 Audit Log

## Events To Audit

- Login Admin.
- Lock User.
- Unlock User.
- Create Staff.
- Update Role.
- Update Branch.
- Update Station.
- Update Device.
- Device Command.
- Order Status Change.
- Order Cancellation.
- Wallet Adjustment.
- System Configuration Change.
- Alert Resolution.

## Audit Information

- Actor ID.
- Actor Role.
- Action.
- Resource Type.
- Resource ID.
- Old Value.
- New Value.
- IP Address.
- User Agent.
- Correlation ID.
- Timestamp.
- Branch Scope.

## Business Rules

- Audit Log không được sửa.
- Audit Log không được xóa bởi Admin thông thường.
- Lưu tối thiểu 12 tháng.
- Dữ liệu nhạy cảm phải được mask.

---

# 4. Out of Scope

Các chức năng sau không thực hiện trong phiên bản hiện tại:

- Mobile App native Android.
- Mobile App native iOS.
- Desktop Client native bằng JavaFX, Electron hoặc .NET.
- Overlay nổi trên game ở cấp hệ điều hành.
- Tự phát triển Voice Codec.
- Hệ thống giải đấu esports.
- Tournament Bracket.
- Livestream.
- Video Platform.
- Marketplace vật phẩm.
- Tích hợp trực tiếp tài khoản game của mọi nhà phát hành.
- AI Matchmaking nâng cao.
- AI Recommendation.
- AI Chatbot.
- Face Recognition.
- eKYC.
- Quản lý kế toán doanh nghiệp đầy đủ.
- Quản lý nhân sự đầy đủ.
- Payroll.
- Inventory Management nâng cao.
- Microservice hoàn chỉnh.
- Kubernetes.
- Kafka.
- Data Warehouse.
- Big Data Analytics.
- Native Anti-cheat Integration.
- Tự động sửa chữa phần cứng.

---

# 5. Frontend Technology

## 5.1 Core Framework

- ReactJS.
- TypeScript.
- Vite.

## 5.2 Routing

- React Router DOM.

## 5.3 HTTP Client

- Axios.
- Axios Interceptor.
- Automatic JWT Header.
- Refresh Token Interceptor.
- Error Response Interceptor.

## 5.4 State Management

- Redux Toolkit.
- RTK Query hoặc TanStack Query cho Server State.
- React Context chỉ dùng cho trạng thái UI đơn giản.

## 5.5 UI Framework

Có thể chọn một trong hai hướng:

### Option A

- Tailwind CSS.
- Headless UI.
- Radix UI.

### Option B

- Ant Design.

Hướng đề xuất cho dự án:

- Tailwind CSS.
- Shadcn/UI.
- Lucide React.

## 5.6 Forms & Validation

- React Hook Form.
- Zod.
- @hookform/resolvers.

## 5.7 Realtime

- WebSocket.
- STOMP.js nếu Backend dùng STOMP.
- SockJS fallback nếu cần.

## 5.8 QR

- html5-qrcode.
- Browser MediaDevices API.
- QRCode.react để tạo QR.

## 5.9 Charts

- Recharts.

## 5.10 Notifications

- Sonner.
- Browser Notification API.
- Service Worker cho PWA Push nếu triển khai.

## 5.11 Loading

- Skeleton Loader.
- React Spinners cho tác vụ dài.

## 5.12 Date & Time

- date-fns hoặc dayjs.

## 5.13 PWA

- vite-plugin-pwa.
- Service Worker.
- Web App Manifest.
- Installable Web App.
- Cache tài nguyên tĩnh.
- Offline fallback có giới hạn.

## 5.14 Frontend Testing

- Vitest.
- React Testing Library.
- Mock Service Worker.
- Playwright cho E2E nếu đủ thời gian.

---

# 6. Backend Technology

## 6.1 Language

- Java 17.

## 6.2 Framework

- Spring Boot 3.

## 6.3 Spring Modules

- Spring Web.
- Spring Security.
- Spring Data JPA.
- Spring Validation.
- Spring WebSocket.
- Spring Actuator.
- Spring Cache.
- Spring Mail nếu dùng email.
- Spring Scheduling.
- Spring Integration MQTT hoặc Eclipse Paho Client.

## 6.4 Authentication & Security

- JWT.
- Access Token.
- Refresh Token.
- Refresh Token Rotation.
- BCrypt hoặc Argon2.
- RBAC.
- Branch Data Scope.
- CORS.
- CSRF disabled cho stateless API.
- Rate Limiting.
- Method Security.
- Global Exception Handler.
- Security Audit Log.

## 6.5 Database

- PostgreSQL.

Có thể sử dụng MySQL nếu môi trường triển khai yêu cầu, nhưng PostgreSQL được khuyến nghị cho dự án này.

## 6.6 Cache

- Redis.

Mục đích:

- Refresh Token.
- QR Session.
- Rate Limit.
- Online User.
- Session Cache.
- Radar Cache.
- Distributed Lock.
- Notification temporary state.

## 6.7 Message & Realtime

- WebSocket.
- STOMP.
- Redis Pub/Sub nếu chạy nhiều instance.
- MQTT cho IoT.

## 6.8 IoT Integration

- MQTT over TLS.
- Eclipse Paho MQTT Client.
- Topic theo Branch, Station và Device.
- Command ACK.
- Heartbeat.
- Telemetry.
- Retry và Timeout.
- Correlation ID.

## 6.9 Object Mapping

- MapStruct.

## 6.10 Utilities

- Lombok.
- @RequiredArgsConstructor.
- Apache Commons.
- Jackson.
- UUID.

## 6.11 API Documentation

- Springdoc OpenAPI.
- Swagger UI.

## 6.12 Database Migration

- Flyway.

## 6.13 Testing

- JUnit 5.
- Mockito.
- AssertJ.
- Spring Boot Test.
- MockMvc.
- Testcontainers.
- WireMock.
- Embedded MQTT hoặc mock adapter.

## 6.14 Build Tool

- Maven.

## 6.15 Development Tools

- Docker.
- Docker Compose.
- Postman.
- Git.
- GitHub.
- IntelliJ IDEA.
- VS Code.
- DBeaver hoặc pgAdmin.

---

# 7. System Architecture

## 7.1 Architecture Style

Phiên bản MVP sử dụng **Modular Monolith**.

Lý do:

- Phù hợp thời gian phát triển ngắn.
- Dễ triển khai.
- Dễ debug.
- Dễ kiểm thử.
- Không tạo overhead như Microservice.
- Vẫn chia module rõ ràng để có thể tách sau này.

## 7.2 High-Level Architecture

```text
+------------------------------------------------------------------+
|                         CLIENT LAYER                             |
+----------------------+----------------------+--------------------+
| Gamer Web / PWA      | Station Kiosk Web   | Admin / Staff Web  |
+----------------------+----------------------+--------------------+
                  | HTTPS / REST / WebSocket
                  v
+------------------------------------------------------------------+
|                      SPRING BOOT BACKEND                          |
+------------------------------------------------------------------+
| Auth | Profile | Session | LFG | Lobby | Order | IoT | Reporting |
+------------------------------------------------------------------+
       |             |              |              |
       v             v              v              v
 PostgreSQL        Redis         MQTT Broker   External Services
```

## 7.3 Deployment Architecture

```text
Browser / PWA
      |
      | HTTPS
      v
Nginx / Reverse Proxy
      |
      +------ React Static Files
      |
      +------ Spring Boot API
                   |
                   +------ PostgreSQL
                   +------ Redis
                   +------ MQTT Broker
                   +------ Payment Gateway
                   +------ Voice Provider
```

---

# 8. Backend Coding Architecture

## 8.1 Package Structure

```text
com.nexus
├── common
│   ├── response
│   ├── exception
│   ├── validation
│   ├── util
│   ├── constants
│   └── audit
├── config
├── security
│   ├── jwt
│   ├── filter
│   ├── handler
│   └── service
├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
├── user
├── branch
├── station
├── session
├── profile
├── game
├── matchmaking
├── lobby
├── order
├── wallet
├── iot
├── notification
├── report
└── audit
```

## 8.2 Module Structure

Mỗi module ưu tiên cấu trúc:

```text
module
├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── mapper
├── specification
├── validator
└── event
```

## 8.3 Backend Coding Rules

- Controller không chứa Business Logic.
- Service xử lý nghiệp vụ.
- Repository chỉ truy cập dữ liệu.
- Không trả Entity trực tiếp ra API.
- Sử dụng Request DTO và Response DTO.
- Mapping bằng MapStruct.
- Constructor Injection.
- Ưu tiên `@RequiredArgsConstructor`.
- Không sử dụng Field Injection.
- Validation bằng annotation và custom validator.
- Exception được xử lý tập trung.
- API response có cấu trúc thống nhất.
- Mọi request quan trọng có correlationId.
- Các thao tác tạo mới quan trọng hỗ trợ idempotency.
- Giao dịch tài chính phải dùng `@Transactional`.
- Query danh sách phải hỗ trợ pagination.
- Query Admin phải kiểm tra data scope.
- Không log token, mật khẩu và thông tin nhạy cảm.

---

# 9. Frontend Coding Architecture

## 9.1 Folder Structure

```text
src
├── app
│   ├── router
│   ├── store
│   └── providers
├── assets
├── components
│   ├── common
│   ├── forms
│   ├── feedback
│   └── charts
├── layouts
│   ├── GamerLayout
│   ├── StationLayout
│   ├── StaffLayout
│   └── AdminLayout
├── features
│   ├── auth
│   ├── profile
│   ├── station
│   ├── session
│   ├── matchmaking
│   ├── lobby
│   ├── order
│   ├── iot
│   ├── dashboard
│   └── notification
├── hooks
├── lib
├── services
├── types
├── utils
├── constants
└── styles
```

## 9.2 Frontend Coding Rules

- Sử dụng Functional Component.
- TypeScript Strict Mode.
- Không sử dụng `any` nếu không cần thiết.
- Component phải có trách nhiệm rõ ràng.
- Tách UI Component và Business Component.
- Tái sử dụng Form Component.
- Route phải có Protected Route.
- Route phải kiểm tra Role.
- API gọi qua Axios Instance dùng chung.
- Access Token được gắn tự động.
- Refresh Token được xử lý tập trung.
- Không lưu thông tin nhạy cảm trong localStorage nếu có lựa chọn an toàn hơn.
- Server State dùng Query Library.
- Form dùng React Hook Form và Zod.
- Error hiển thị thân thiện.
- Loading dùng Skeleton.
- Empty State phải rõ ràng.
- Table phải có pagination.
- Giao diện phải responsive.
- Hỗ trợ keyboard navigation ở màn hình quản trị.

---

# 10. UI Design Style

## 10.1 Concept

**Cyber Esports Command Center**

Phong cách:

- Modern.
- Futuristic.
- Gaming.
- Dark Mode.
- Neon Accent.
- Dashboard.
- Glassmorphism có kiểm soát.
- Responsive.
- High Contrast.
- Realtime Status.

## 10.2 Primary Color

```text
Neon Cyan
#00E5FF
```

## 10.3 Secondary Color

```text
Electric Blue
#3B82F6
```

## 10.4 Accent Color

```text
Cyber Purple
#8B5CF6
```

## 10.5 Background

```text
Main Background
#0B1120

Secondary Background
#0F172A
```

## 10.6 Card

```text
Background
#1E293B

Opacity
80% - 95%

Blur
12px - 16px
```

## 10.7 Status Colors

```text
Success
#22C55E

Error
#EF4444

Warning
#FACC15

Info
#38BDF8

Critical
#DC2626
```

## 10.8 Text

```text
Primary
#FFFFFF

Secondary
#CBD5E1

Muted
#94A3B8
```

## 10.9 Border

```text
#334155
```

---

# 11. Application Layout

Toàn bộ ứng dụng sử dụng **Application Shell Layout**.

```text
+--------------------------------------------------------------+
|                         TOP NAVBAR                           |
+----------------------+---------------------------------------+
|                      |                                       |
|                      |                                       |
|      SIDEBAR         |           MAIN CONTENT                |
|                      |                                       |
|                      |                                       |
+----------------------+---------------------------------------+
|                         FOOTER                               |
+--------------------------------------------------------------+
```

Chỉ Main Content thay đổi theo từng route.

Sidebar, Navbar và Footer được tái sử dụng.

## 11.1 Gamer Layout

- Mobile-first.
- Bottom Navigation trên điện thoại.
- Sidebar thu gọn trên Desktop.
- Hiển thị số dư.
- Hiển thị Session hiện tại.
- Notification Bell.
- QR Scanner shortcut.

## 11.2 Station Kiosk Layout

- Fullscreen.
- Không có Sidebar truyền thống.
- QR Login là màn hình mặc định.
- Sau đăng nhập hiển thị Session Panel.
- Nút mở Radar.
- Nút mở Quick Order.
- Nút xem Smart Station.
- Nút kết thúc Session.
- Hạn chế điều hướng ngoài hệ thống.

## 11.3 Staff Layout

- Sidebar.
- Order Queue là màn hình chính.
- Badge số đơn mới.
- Âm báo.
- Quick Status Actions.
- Alert panel.

## 11.4 Admin Layout

- Sidebar nhiều cấp.
- Dashboard.
- Data Table.
- Filter.
- Export.
- Audit.
- Configuration.

---

# 12. Main UI Components

## Common Components

- App Sidebar.
- Top Navbar.
- Footer.
- Breadcrumb.
- Page Header.
- Data Table.
- Pagination.
- Search Box.
- Filter Panel.
- Date Range Picker.
- Dropdown.
- Modal.
- Drawer.
- Confirmation Dialog.
- Toast Notification.
- Loading Skeleton.
- Empty State.
- Error State.
- Status Badge.
- Role Badge.
- Avatar.
- Notification Bell.
- Protected Route.
- Role Guard.

## Gamer Components

- QR Scanner.
- Wallet Card.
- Active Session Card.
- Smart Station Setting Form.
- Game Profile Card.
- LFG Signal Form.
- Gamer Match Card.
- Invitation Popup.
- Lobby Member List.
- Chat Panel.
- Menu Item Card.
- Cart Drawer.
- Order Tracking Timeline.

## Staff Components

- Order Queue.
- Order Detail Drawer.
- Status Action Buttons.
- SLA Timer.
- Station Delivery Badge.
- Device Alert Queue.
- Alert Detail Panel.

## Admin Components

- KPI Card.
- Revenue Chart.
- Occupancy Chart.
- Station Heatmap.
- Device Health Chart.
- User Management Table.
- Station Management Table.
- Device Management Table.
- Audit Log Table.
- Report Export Panel.

---

# 13. Responsive Requirements

## Desktop

```text
>= 1200px
```

- Full Sidebar.
- Multi-column Dashboard.
- Data Table đầy đủ.
- Heatmap chi tiết.

## Laptop

```text
992px - 1199px
```

- Sidebar thu gọn.
- Dashboard 2 hoặc 3 cột.
- Table có horizontal scroll.

## Tablet

```text
768px - 991px
```

- Drawer Navigation.
- Dashboard 2 cột.
- Form chia section.

## Mobile

```text
>= 375px
```

- Mobile-first.
- Bottom Navigation cho Gamer.
- Card Layout thay cho bảng phức tạp.
- QR Scanner toàn màn hình.
- Button tối thiểu 44px.
- Không dùng hover làm tương tác chính.

---

# 14. API Design Standard

## 14.1 Base URL

```text
/api/v1
```

## 14.2 Standard Response

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-07-17T10:00:00Z",
  "correlationId": "uuid"
}
```

## 14.3 Standard Error Response

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "details": {},
  "timestamp": "2026-07-17T10:00:00Z",
  "correlationId": "uuid"
}
```

## 14.4 Pagination Response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

# 15. Main API Endpoints

## Authentication

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
POST   /api/v1/auth/change-password
```

## QR Login

```text
POST   /api/v1/auth/qr-sessions
GET    /api/v1/auth/qr-sessions/{id}
POST   /api/v1/auth/qr-sessions/{id}/confirm
POST   /api/v1/auth/qr-sessions/{id}/cancel
```

## Profile

```text
GET    /api/v1/profiles/me
PUT    /api/v1/profiles/me
POST   /api/v1/profiles/me/avatar
GET    /api/v1/profiles/me/game-profiles
POST   /api/v1/profiles/me/game-profiles
PUT    /api/v1/profiles/me/game-profiles/{id}
DELETE /api/v1/profiles/me/game-profiles/{id}
```

## Smart Station

```text
GET    /api/v1/station-preferences/me
PUT    /api/v1/station-preferences/me
POST   /api/v1/station-preferences/me/reset
POST   /api/v1/iot/stations/{stationId}/apply-profile
GET    /api/v1/iot/commands/{commandId}
```

## Session

```text
GET    /api/v1/sessions/current
GET    /api/v1/sessions/history
POST   /api/v1/sessions/{id}/end
GET    /api/v1/admin/sessions
```

## LFG

```text
POST   /api/v1/lfg/signals
GET    /api/v1/lfg/signals
GET    /api/v1/lfg/signals/me
PUT    /api/v1/lfg/signals/{id}
DELETE /api/v1/lfg/signals/{id}
POST   /api/v1/lfg/signals/{id}/renew
```

## Invitation

```text
POST   /api/v1/team-invitations
GET    /api/v1/team-invitations/received
GET    /api/v1/team-invitations/sent
PATCH  /api/v1/team-invitations/{id}/accept
PATCH  /api/v1/team-invitations/{id}/reject
PATCH  /api/v1/team-invitations/{id}/cancel
```

## Lobby

```text
GET    /api/v1/lobbies/{id}
POST   /api/v1/lobbies/{id}/members
DELETE /api/v1/lobbies/{id}/members/{userId}
POST   /api/v1/lobbies/{id}/leave
POST   /api/v1/lobbies/{id}/close
GET    /api/v1/lobbies/{id}/messages
POST   /api/v1/lobbies/{id}/messages
```

## Menu & Order

```text
GET    /api/v1/menu/categories
GET    /api/v1/menu/items
GET    /api/v1/menu/items/{id}
POST   /api/v1/orders
GET    /api/v1/orders/me
GET    /api/v1/orders/{id}
POST   /api/v1/orders/{id}/cancel
```

## Staff Order

```text
GET    /api/v1/staff/orders
GET    /api/v1/staff/orders/{id}
PATCH  /api/v1/staff/orders/{id}/status
PATCH  /api/v1/staff/orders/{id}/assign
```

## Admin Station & Device

```text
GET    /api/v1/admin/branches
POST   /api/v1/admin/branches
GET    /api/v1/admin/stations
POST   /api/v1/admin/stations
PATCH  /api/v1/admin/stations/{id}
GET    /api/v1/admin/devices
POST   /api/v1/admin/devices
PATCH  /api/v1/admin/devices/{id}
```

## Alerts

```text
GET    /api/v1/staff/device-alerts
GET    /api/v1/staff/device-alerts/{id}
PATCH  /api/v1/staff/device-alerts/{id}/acknowledge
PATCH  /api/v1/staff/device-alerts/{id}/resolve
PATCH  /api/v1/staff/device-alerts/{id}/close
```

## Dashboard & Report

```text
GET    /api/v1/admin/dashboard/overview
GET    /api/v1/admin/reports/revenue
GET    /api/v1/admin/reports/sessions
GET    /api/v1/admin/reports/orders
GET    /api/v1/admin/reports/devices
GET    /api/v1/admin/reports/export
```

## Notification

```text
GET    /api/v1/notifications
PATCH  /api/v1/notifications/{id}/read
PATCH  /api/v1/notifications/read-all
DELETE /api/v1/notifications/{id}
```

## Audit

```text
GET    /api/v1/admin/audit-logs
GET    /api/v1/admin/audit-logs/{id}
```

---

# 16. Database Scope

## Main Tables

- users.
- roles.
- permissions.
- user_roles.
- refresh_tokens.
- password_reset_tokens.
- branches.
- zones.
- stations.
- gamer_profiles.
- station_preferences.
- games.
- game_ranks.
- game_roles.
- gamer_game_profiles.
- play_sessions.
- qr_login_sessions.
- lfg_signals.
- team_invitations.
- lobbies.
- lobby_members.
- lobby_messages.
- menu_categories.
- menu_items.
- food_orders.
- order_items.
- wallets.
- wallet_transactions.
- iot_devices.
- device_commands.
- device_telemetry.
- device_alerts.
- notifications.
- user_blocks.
- user_reports.
- audit_logs.

## Database Rules

- Sử dụng UUID hoặc Long thống nhất.
- Mọi bảng nghiệp vụ có `created_at`.
- Các bảng cần cập nhật có `updated_at`.
- Dữ liệu quản trị có `created_by`, `updated_by`.
- Không xóa cứng User, Station, Device, Menu Item.
- Dùng status hoặc soft delete.
- Order Item lưu `unit_price`.
- Wallet Transaction không được sửa.
- Audit Log không được sửa.
- Telemetry có thể lưu riêng nếu dữ liệu lớn.
- Timestamp lưu UTC.
- Hiển thị theo timezone chi nhánh.

---

# 17. Security Requirements

- HTTPS bắt buộc trong Production.
- TLS 1.2 trở lên.
- Password Hash bằng BCrypt hoặc Argon2.
- JWT Access Token ngắn hạn.
- Refresh Token Rotation.
- Token Revocation.
- RBAC.
- Method Security.
- Branch Data Scope.
- Rate Limit.
- CORS whitelist.
- Validation toàn bộ input.
- Chống SQL Injection thông qua JPA parameter binding.
- Chống XSS.
- Chống IDOR.
- Không trả stack trace ra Client.
- Không log mật khẩu.
- Không log token.
- Mask email và số điện thoại trong log.
- Payment Webhook phải xác thực chữ ký.
- Voice Webhook phải xác thực chữ ký.
- Chống replay callback.
- MFA được khuyến nghị cho Admin.
- Audit thao tác nhạy cảm.
- Dependency Scan.
- OWASP Top 10 Review trước Go-live.

---

# 18. Non-Functional Requirements

## Performance

- 95% REST API phản hồi dưới 500ms.
- QR Login hoàn thành dưới 5 giây.
- Notification realtime dưới 3 giây.
- Smart Station hoàn thành dưới 10 giây.
- API danh sách bắt buộc pagination.
- Dashboard không tải toàn bộ dữ liệu thô.

## Availability

- Backend MVP đạt 99.5% mỗi tháng.
- Có health check.
- Có retry cho tích hợp ngoài.
- Có fallback khi Voice Provider lỗi.
- Lỗi IoT không chặn phiên chơi.

## Scalability

- Hỗ trợ tối thiểu 20 chi nhánh.
- Hỗ trợ tối thiểu 2.000 Station.
- Hỗ trợ tối thiểu 10.000 User online theo kiến trúc mở rộng.
- WebSocket có thể mở rộng bằng Redis Pub/Sub.

## Observability

- Centralized Logging.
- Metrics.
- Health Check.
- Correlation ID.
- Error Tracking.
- Alert theo SLA.
- Spring Boot Actuator.

## Compatibility

### Browser

- Google Chrome phiên bản mới.
- Microsoft Edge phiên bản mới.
- Safari Mobile phiên bản mới.
- Android Chrome phiên bản mới.

### Screen

- 1920x1080.
- 2560x1440.
- 1366x768.
- Tablet.
- Mobile từ 375px.

## Backup

- RPO nhỏ hơn hoặc bằng 15 phút.
- RTO nhỏ hơn hoặc bằng 4 giờ.
- Backup Database định kỳ.
- Kiểm tra khôi phục dữ liệu.

---

# 19. Testing Scope

## 19.1 Unit Test

### Service Layer

Thực hiện Unit Test cho:

- AuthenticationService.
- RefreshTokenService.
- QRLoginService.
- GamerProfileService.
- StationPreferenceService.
- PlaySessionService.
- LfgService.
- TeamInvitationService.
- LobbyService.
- OrderService.
- WalletService.
- DeviceService.
- DeviceAlertService.
- DashboardService.
- NotificationService.
- AuditService.

Nội dung kiểm thử:

- Business Logic.
- Validation.
- Authorization.
- State Machine.
- Repository Mock.
- Mapper.
- Success Case.
- Failure Case.
- Boundary Case.
- Duplicate Request.
- Idempotency.

## 19.2 Controller Test

Sử dụng MockMvc.

Kiểm thử:

- HTTP Status.
- Response Body.
- Validation.
- Authentication.
- Authorization.
- Role.
- Branch Scope.
- Exception Handler.
- Pagination.
- Invalid ID.
- Forbidden Access.

## 19.3 Integration Test

Sử dụng Testcontainers cho:

- PostgreSQL.
- Redis.
- MQTT nếu khả thi.

Kiểm thử:

- Repository.
- Transaction.
- JWT Filter.
- QR Login Flow.
- Wallet Transaction.
- Order State.
- Device Heartbeat.
- Alert Creation.

## 19.4 End-to-End Test

Các luồng quan trọng:

1. Register -> Login -> Update Profile.
2. Station tạo QR -> Gamer quét -> Tạo Session.
3. Session Active -> Apply Smart Station.
4. Tạo LFG -> Gửi Invitation -> Tạo Lobby.
5. Chọn món -> Tạo Order -> Staff xử lý -> Delivered.
6. Device mất heartbeat -> Tạo Alert -> Staff Resolve.
7. Admin xem Dashboard -> Export Report.

## 19.5 Security Test

- Login brute force.
- Token replay.
- Refresh Token reuse.
- IDOR.
- Role bypass.
- Branch scope bypass.
- XSS.
- Invalid WebSocket authentication.
- Fake Payment Webhook.
- Fake MQTT message.
- Rate Limit.

## 19.6 Performance Test

- Login API.
- QR Confirm.
- Radar Query.
- Notification.
- Order Creation.
- Dashboard.
- WebSocket concurrent connection.

## 19.7 Coverage Target

- Service Layer >= 80%.
- Controller Layer >= 75%.
- Business Critical Module >= 85%.

---

# 20. Acceptance Criteria Summary

## Authentication

- Gamer đăng ký với dữ liệu hợp lệ thành công.
- Email trùng bị từ chối.
- Login đúng trả Access Token và Refresh Token.
- Login sai không trả token.
- Refresh Token đã revoke không dùng được.
- Gamer không truy cập Admin API.

## QR Login

- QR hợp lệ tạo đúng một Session Active.
- QR hết hạn bị từ chối.
- QR đã dùng không dùng lại.
- Retry không tạo trùng Session.
- Station bận không tạo Session mới.

## Smart Station

- Cấu hình hợp lệ được gửi đến IoT Gateway.
- Lỗi một thiết bị không chặn phiên chơi.
- Giá trị vượt giới hạn bị từ chối.
- Critical Error dừng lệnh cơ khí.

## LFG

- Chỉ Gamer Active xuất hiện.
- Signal hết hạn tự đóng.
- Người bị block không xuất hiện.
- Không lộ dữ liệu nhạy cảm.

## Invitation & Lobby

- Accept Invitation tạo Lobby.
- Invitation hết hạn không Accept được.
- Voice lỗi không làm Lobby text lỗi.
- Lobby không vượt giới hạn thành viên.

## Order

- Hết hàng không tạo đơn.
- Giá snapshot chính xác.
- Không đủ số dư không trừ tiền.
- Trạng thái phải đúng state machine.
- Hủy đơn đã thanh toán phải hoàn tiền.

## IoT Alert

- Mất 3 heartbeat tạo Alert.
- Critical Alert khóa điều khiển cần thiết.
- Resolve lưu Staff và thời gian.
- Device Update có Audit Log.

## Admin

- Branch Admin chỉ xem đúng chi nhánh.
- Super Admin xem được nhiều chi nhánh.
- Dashboard hiển thị đúng bộ lọc.
- Export đúng phạm vi dữ liệu.

---

# 21. Development Timeline

## Day 1 - 07/07/2026

- Khởi tạo Backend.
- Khởi tạo Frontend.
- Cấu hình Database.
- Thiết kế cấu trúc module.
- Thiết kế Entity cốt lõi.
- Cấu hình Docker Compose.
- Cấu hình Git Repository.

## Day 2 - 08/07/2026

- Authentication.
- JWT.
- Refresh Token.
- Role.
- Security Configuration.
- Register.
- Login.
- Logout.
- Global Exception Handler.

## Day 3 - 09/07/2026

- User Profile.
- Branch.
- Zone.
- Station.
- Game Profile.
- Station Preference.
- Frontend Login và Layout.

## Day 4 - 10/07/2026

- QR Login.
- Play Session.
- Station Kiosk Web.
- WebSocket kết nối Station.
- Session UI.
- Unit Test Authentication và Session.

## Day 5 - 11/07/2026

- LFG.
- Team Invitation.
- Lobby.
- Notification.
- Gamer Radar UI.
- Realtime Invitation.

## Day 6 - 12/07/2026

- Menu.
- Cart.
- Food Order.
- Wallet.
- Staff Order Queue.
- Order State Machine.
- Realtime Order Notification.

## Day 7 - 13/07/2026

- IoT Device.
- MQTT.
- Device Command.
- Heartbeat.
- Device Alert.
- Staff Technical UI.

## Day 8 - 14/07/2026

- Admin Dashboard.
- Reports.
- Audit Log.
- User Management.
- Station Management.
- Device Management.

## Day 9 - 15/07/2026

- Frontend Integration.
- Responsive.
- PWA.
- Loading.
- Error Handling.
- Role-Based UI.
- Branch Scope.

## Day 10 - 16/07/2026

- Unit Test.
- Integration Test.
- Fix Bug.
- Security Review.
- Postman Collection.
- Swagger Documentation.

## Day 11 - 17/07/2026

- Final Testing.
- UAT.
- Hoàn thiện tài liệu.
- Chuẩn bị Demo.
- Build Production.
- Tổng kết và bàn giao.

---

# 22. Documentation Requirements

Dự án phải có:

- `README.md`
- `SRS.md`
- `Project-Scope.md`
- `System-Architecture.md`
- `System-Flow.md`
- `Database-Design.md`
- `API-Documentation.md`
- `Prompt-Chain.md`
- `Test-Plan.md`
- `Test-Report.md`
- `Changelog.md`
- `Error-Log.md`
- `Deployment-Guide.md`
- `Postman-Guide.md`

---

# 23. Error Log Documentation

Mọi lỗi thực tế phát sinh phải được ghi vào `Error-Log.md`.

## Error Log Structure

- Error ID.
- Date.
- Category.
- Module.
- Feature.
- Environment.
- Error Description.
- Reproduction Steps.
- Expected Result.
- Actual Result.
- Root Cause.
- Resolution.
- Lesson Learned.
- Status.
- Related Commit.
- Related Issue.

## Error Log Template

```md
# ERR-001

## Date
2026-07-08

## Category
Authentication

## Module
Frontend Authentication

## Feature
JWT Request Interceptor

## Environment
Local Development

## Error Description
JWT Access Token không được gửi trong Authorization Header.

## Reproduction Steps
1. Login thành công.
2. Truy cập API `/api/v1/profiles/me`.
3. Backend trả về 401 Unauthorized.

## Expected Result
Request có Header `Authorization: Bearer <token>`.

## Actual Result
Request không có Authorization Header.

## Root Cause
Axios Instance chưa cấu hình Request Interceptor.

## Resolution
Tạo Axios Instance dùng chung và tự động gắn Access Token.

## Lesson Learned
Mọi API cần xác thực phải sử dụng chung một Axios Instance.

## Status
Resolved
```

## Error Categories

- Backend.
- Frontend.
- Database.
- Authentication.
- Authorization.
- API.
- WebSocket.
- MQTT.
- IoT.
- UI/UX.
- Performance.
- Deployment.
- Testing.
- Security.

## Documentation Rules

- Ghi lỗi ngay khi phát sinh.
- Chỉ ghi lỗi thực tế.
- Không ghi lỗi giả lập.
- Mỗi lỗi chỉ ghi một lần.
- Sau khi sửa phải cập nhật trạng thái.
- Ghi rõ ảnh hưởng.
- Gắn commit hoặc issue nếu có.
- Không đưa token hoặc mật khẩu vào Error Log.

---

# 24. Git Workflow

## Branches

```text
main
develop
feature/*
fix/*
hotfix/*
release/*
```

## Commit Convention

```text
feat: add QR login flow
fix: prevent duplicate active session
refactor: simplify order state validation
test: add LFG service unit tests
docs: update project scope
chore: configure docker compose
```

## Pull Request Rules

- Không push trực tiếp lên main.
- Pull Request phải có mô tả.
- Code phải build thành công.
- Test quan trọng phải pass.
- Không commit file `.env`.
- Không commit secret.
- Không commit build folder.
- Resolve conflict trước khi merge.

---

# 25. Environment Configuration

## Backend Environment Variables

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_ACCESS_EXPIRATION
JWT_REFRESH_EXPIRATION
REDIS_HOST
REDIS_PORT
MQTT_BROKER_URL
MQTT_USERNAME
MQTT_PASSWORD
PAYMENT_WEBHOOK_SECRET
VOICE_PROVIDER_KEY
CORS_ALLOWED_ORIGINS
```

## Frontend Environment Variables

```text
VITE_API_BASE_URL
VITE_WS_BASE_URL
VITE_APP_NAME
VITE_ENABLE_PWA
VITE_ENABLE_VOICE
```

## Security Rules

- Không commit `.env`.
- Tạo `.env.example`.
- Secret Production lưu trong secret manager.
- Không hard-code JWT secret.
- Không hard-code database password.

---

# 26. Deployment Scope

## Development

- Frontend chạy Vite.
- Backend chạy Spring Boot.
- PostgreSQL bằng Docker.
- Redis bằng Docker.
- MQTT Broker bằng Docker.

## Production

- Frontend build static.
- Nginx serve frontend.
- Nginx reverse proxy API.
- Spring Boot chạy Docker Container.
- PostgreSQL tách volume.
- Redis tách volume.
- MQTT Broker cấu hình TLS.
- HTTPS bằng SSL Certificate.
- Log được thu thập tập trung.

## Docker Compose Services

```text
frontend
backend
postgres
redis
mqtt
nginx
```

---

# 27. Risks & Mitigation

## IoT Protocol Inconsistency

**Risk:** Thiết bị nhiều hãng dùng giao thức khác nhau.

**Mitigation:**

- Dùng IoT Gateway.
- Adapter Pattern.
- Chuẩn hóa command nội bộ.

## Mechanical Safety

**Risk:** Bàn hoặc ghế vận hành sai gây mất an toàn.

**Mitigation:**

- Validate Backend.
- Hard Limit Firmware.
- Emergency Stop.
- Critical Alert.

## Unstable Branch Network

**Risk:** Mạng phòng máy gián đoạn.

**Mitigation:**

- Retry.
- Cache.
- Idempotency.
- Local fallback.
- Degraded Mode.

## Web Limitation

**Risk:** Web không tạo overlay ổn định trên game như Desktop Native.

**Mitigation:**

- Dùng Quick Order Panel.
- Dùng tab hoặc cửa sổ riêng.
- Dùng phím tắt trình duyệt.
- Không cam kết overlay nổi trên mọi game.

## Voice Provider Failure

**Risk:** Voice Chat không hoạt động.

**Mitigation:**

- Text Chat vẫn hoạt động.
- Circuit Breaker.
- Hiển thị trạng thái rõ ràng.

## Inventory Mismatch

**Risk:** Tồn kho thực tế khác hệ thống.

**Mitigation:**

- Kiểm tra tồn kho khi tạo đơn.
- Reservation.
- Audit điều chỉnh tồn kho.

## Radar Abuse

**Risk:** Spam lời mời hoặc quấy rối.

**Mitigation:**

- Rate Limit.
- Block.
- Report.
- Moderation.
- Audit Log.

---

# 28. Future Expansion

Có thể mở rộng:

- Native Mobile App.
- Native Desktop Client.
- System Overlay.
- AI Matchmaking.
- Recommendation Engine.
- Tournament Management.
- Esports Bracket.
- Loyalty Point.
- Membership.
- Subscription.
- Dynamic Pricing.
- Promotion Engine.
- Advanced Inventory.
- Multi-tenant SaaS.
- Microservices.
- Kafka Event Streaming.
- Kubernetes.
- Central Data Warehouse.
- AI Predictive Maintenance.
- Computer Vision.
- Face Login.
- Smart Access Control.
- Mobile Push Notification.
- Cloud Deployment nhiều vùng.

---

# 29. Definition of Done

Một chức năng được xem là hoàn thành khi:

- Đúng yêu cầu nghiệp vụ.
- API hoạt động.
- Validation đầy đủ.
- Phân quyền đúng.
- UI hoạt động.
- Responsive.
- Loading và Error State đầy đủ.
- Unit Test quan trọng đã viết.
- Không có lỗi Critical.
- Swagger đã cập nhật.
- Postman đã cập nhật.
- Error Log đã cập nhật nếu có lỗi.
- Code đã review.
- Commit đúng convention.
- Không có secret trong source code.
- Có thể demo được.

---

# 30. Final Project Deliverables

- Source Code Frontend.
- Source Code Backend.
- Database Migration.
- Docker Compose.
- Postman Collection.
- Swagger API.
- SRS.
- Project Scope.
- ERD.
- Use Case Diagram.
- Activity Diagram.
- Sequence Diagram.
- Architecture Diagram.
- Test Plan.
- Test Report.
- Error Log.
- Changelog.
- Deployment Guide.
- Demo Script.
- Final Presentation.
