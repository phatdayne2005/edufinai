## Đề cương báo cáo Auth-Service (Outline dùng cho file .docx)

> Gợi ý: Khi tạo file Word (`.docx`), hãy map các mục dưới đây vào Heading 1, Heading 2, Heading 3 để tạo mục lục tự động.

---

## 1. User Requirement (Đặc tả yêu cầu người dùng)

### 1.1. Giới thiệu hệ thống EduFinAI
- Mô tả ngắn gọn về hệ thống EduFinAI.
- Vai trò của Auth-Service trong kiến trúc tổng thể.

### 1.2. Đối tượng sử dụng hệ thống
- Người học (Learner).
- Người tạo nội dung (Creator).
- Quản trị viên (Admin).

### 1.3. Nhu cầu và mục tiêu của người dùng
- Đăng ký / đăng nhập / đăng xuất.
- Duy trì phiên đăng nhập ổn định (không bị văng ra khi đang sử dụng).
- Quên mật khẩu, lấy lại mật khẩu qua email.
- Xem và chỉnh sửa thông tin cá nhân.
- Quản trị người dùng (dành cho Admin).

### 1.4. Use Case tổng quan
- Danh sách các Use Case chính (Login, Register, Logout, Refresh Token, Forgot Password, View Profile, Update Profile, Manage Users).
- Sơ đồ Use Case (PlantUML) – chèn code:
  - `UseCase Diagram (Auth-Service)`

---

## 2. Software Requirement Specification (SRS)

### 2.1. Phạm vi của Auth-Service
- Mục tiêu của Auth-Service.
- Các hệ thống bên ngoài tương tác với Auth-Service (Gateway, các service khác, Email server).

### 2.2. Yêu cầu chức năng (Functional Requirements)
- FR-01 → FR-xx cho:
  - Đăng nhập & cấp JWT.
  - Gia hạn token (Refresh).
  - Đăng xuất (Logout).
  - Quên mật khẩu & OTP.
  - Quản lý người dùng (CRUD User).
  - Quản lý vai trò & quyền (Role/Permission).

### 2.3. Yêu cầu phi chức năng (Non-Functional Requirements)
- Bảo mật (hash mật khẩu, ký token, bảo mật giao tiếp).
- Hiệu năng & khả năng mở rộng.
- Tính sẵn sàng và khả năng tích hợp.
- Logging & giám sát.

### 2.4. Các ràng buộc và giả định
- Công nghệ (Java, Spring Boot, MySQL).
- Kiến trúc microservices.
- Giả định về môi trường triển khai và người dùng.

---

## 3. Architecture Design (Thiết kế kiến trúc)

### 3.1. Kiến trúc tổng thể hệ thống
- Mối quan hệ giữa:
  - Client (Frontend).
  - API Gateway.
  - Auth-Service.
  - Các service nghiệp vụ khác (learning, finance, gamification,…).
  - Database & Email server.
- Đặc điểm kiến trúc: microservices, stateless authentication với JWT.

### 3.2. Các thành phần chính của Auth-Service
- Controller layer:
  - `AuthenticationController`.
  - `UserController`.
- Service layer:
  - `AuthenticationService`.
  - `EmailService`, `OtpService`.
- Repository layer:
  - `UserRepository`, `RoleRepository`, `PermissionRepository`, `InvalidatedTokenRepository`.
- Security & Config:
  - `SecurityConfig`, `CustomJwtDecoder`, `JwtAuthenticationEntryPoint`.

### 3.3. Sơ đồ kiến trúc (Component/Deployment)
- Mô tả text.
- Sơ đồ kiến trúc / triển khai (PlantUML):
  - `Component/Deployment Diagram (Client – Gateway – Auth-Service – DB)`.

---

## 4. Detail Design (Thiết kế chi tiết)

### 4.1. Thiết kế cơ sở dữ liệu (ERD – logic)
- Các bảng chính:
  - `User`.
  - `Role`.
  - `Permission`.
  - `InvalidatedToken`.
- Quan hệ:
  - User – Role: N–N.
  - Role – Permission: N–N.
- Ghi chú:
  - Lưu ý về khóa chính, chỉ mục, ràng buộc unique (username, email,…).

### 4.2. Class Diagram (mô hình lớp)
- Các entity:
  - `User`, `Role`, `Permission`, `InvalidatedToken`.
- Các lớp service & controller chính:
  - `AuthenticationService`, `AuthenticationController`, `UserController`.
- Sơ đồ lớp (PlantUML):
  - `Class Diagram (Entities + Services + Controllers)`.

### 4.3. Thiết kế luồng nghiệp vụ (Sequence Diagrams)
- Đăng nhập (Login):
  - Client → AuthenticationController → AuthenticationService → UserRepository → JWT Provider.
  - Trường hợp thành công / thất bại.
- Gia hạn token (Refresh Token):
  - Client → AuthenticationController → AuthenticationService → InvalidatedTokenRepository → UserRepository → JWT Provider.
- (Tuỳ chọn) Quên mật khẩu & OTP:
  - Client → AuthenticationController → OtpService → EmailService.
- Đính kèm các sơ đồ (PlantUML):
  - `Sequence Diagram – Login`.
  - `Sequence Diagram – Refresh Token`.

### 4.4. Activity Diagram
- Luồng xử lý request có JWT:
  - Kiểm tra có token.
  - Verify chữ ký.
  - Kiểm tra hạn dùng.
  - Kiểm tra blacklist (InvalidatedToken).
  - Gắn SecurityContext, chuyển tiếp đến controller.
- Sơ đồ hoạt động (PlantUML):
  - `Activity Diagram – Xử lý request với JWT`.

### 4.5. State Diagram
- Vòng đời JWT Token:
  - Active → Expired → Refreshable → Dead.
  - Active → Invalidated (khi logout hoặc refresh).
- Sơ đồ trạng thái (PlantUML):
  - `State Diagram – JWT Lifecycle`.

---

## 5. System Implementation (Mã nguồn)

### 5.1. Công nghệ và thư viện sử dụng
- Java, Spring Boot, Spring Security.
- Spring Data JPA, Hibernate.
- JWT (Nimbus JOSE + JWT).
- BCryptPasswordEncoder.
- Lombok, Maven.

### 5.2. Cấu trúc thư mục mã nguồn Auth-Service
- `controller/`
- `service/`
- `entity/`
- `repository/`
- `configuration/`
- `exception/`, `dto/`, …

### 5.3. Mô tả chi tiết một số thành phần chính
- `AuthenticationService`:
  - `authenticate()`, `refreshToken()`, `logout()`, `introspect()`, `forgotPassword()`, `resetPassword()`.
- `SecurityConfig` & `CustomJwtDecoder`:
  - Cách cấu hình resource server, public endpoints, decoder JWT.
- `UserController`:
  - Các API CRUD user & `/users/my-info`.

### 5.4. Trích dẫn đoạn mã minh họa
- Ví dụ:
  - Đoạn sinh JWT trong `AuthenticationService`.
  - Đoạn verify token & kiểm tra `InvalidatedToken`.

---

## 6. Testing Document (Tài liệu kiểm thử)

### 6.1. Chiến lược kiểm thử
- Unit Test:
  - Kiểm thử lớp `AuthenticationService`, `OtpService`, `EmailService`.
- Integration Test:
  - Kiểm thử endpoint `/auth/token`, `/auth/refresh`, `/auth/logout`, `/users/my-info`.
- Security Test:
  - Gọi API không token.
  - Gọi API với token sai / hết hạn / đã logout.

### 6.2. Danh sách Test Case chính
- TC-01: Đăng nhập thành công.
- TC-02: Đăng nhập sai mật khẩu.
- TC-03: Gọi API protected không có token.
- TC-04: Refresh token trong thời gian cho phép.
- TC-05: Refresh token quá thời gian.
- TC-06: Logout rồi dùng lại token cũ.

### 6.3. Kết quả kiểm thử
- Bảng kết quả: Test case, trạng thái (Pass/Fail), ghi chú.
- Nhận xét chung về chất lượng, lỗi còn tồn tại (nếu có).

---

## 7. Installation Guide (Hướng dẫn cài đặt & triển khai)

### 7.1. Yêu cầu môi trường
- JDK, Maven, MySQL.
- Cấu hình biến môi trường (nếu có).

### 7.2. Cấu hình database & file `application.yaml`
- Thông số kết nối DB.
- Cấu hình JWT (`signerKey`, `valid-duration`, `refreshable-duration`).
- Cấu hình mail server cho OTP (host, port, username, password).

### 7.3. Các bước build & chạy Auth-Service
- Clone project.
- Chạy lệnh Maven build.
- Chạy bằng `mvn spring-boot:run` hoặc `java -jar ...`.

### 7.4. Tích hợp với API Gateway & các service khác
- Cấu hình route trên Gateway.
- Base URL cho frontend.
- Cách truyền header `Authorization: Bearer <token>` từ frontend.

### 7.5. Hướng dẫn sử dụng cơ bản
- Gọi thử API đăng ký / đăng nhập.
- Lấy token và gọi các API yêu cầu xác thực.
- Thử luồng refresh token & logout.

---

## 8. Phụ lục (PlantUML Code)

> Gợi ý: Tạo một file riêng `auth-service-diagrams.puml` trong đó chứa toàn bộ PlantUML cho: Use Case, Class, Sequence, Activity, State.

- 8.1. Use Case Diagram – Auth-Service.
- 8.2. Class Diagram – Entities & Services.
- 8.3. Sequence Diagram – Login.
- 8.4. Sequence Diagram – Refresh Token.
- 8.5. Activity Diagram – Xử lý request với JWT.
- 8.6. State Diagram – JWT Lifecycle.


