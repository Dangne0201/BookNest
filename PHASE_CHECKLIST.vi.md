# Checklist nghiệm thu các giai đoạn

Dùng checklist này để rà soát từng giai đoạn. Chỉ đánh dấu hoàn thành khi đã kiểm tra trực tiếp code hoặc kết quả lệnh. Nếu chưa xác minh được mục nào, để trống và ghi rõ lý do trong `AGENTS.md`.

**Bộ version người dùng đã xác nhận:** Java 17, Spring Boot 4.0.8, PostgreSQL 17, Maven 3.9.16 và Maven Wrapper 3.3.4. Hãy hỏi trước khi thay đổi các version này.

## Phase 0 — Nền tảng dự án

- [x] Khung Spring Boot được cấu hình cho Java 17.
- [x] Có script và cấu hình Maven Wrapper.
- [x] Cấu hình kết nối PostgreSQL được truyền qua biến môi trường.
- [x] Docker Compose định nghĩa ứng dụng và PostgreSQL với named volume riêng cho BookNest.
- [x] PostgreSQL có health check và ứng dụng chờ database khỏe trước khi khởi động.
- [x] Cổng ứng dụng chỉ bind vào loopback; PostgreSQL không publish cổng ra host.
- [x] Dockerfile multi-stage build bằng Java 17 và chạy runtime bằng user không phải root.
- [x] Actuator readiness health có bao gồm health indicator của database.
- [x] README hướng dẫn chạy local, health URL, dừng ứng dụng, chạy test và giới hạn hiện tại.
- [x] Giá trị local-only được ghi rõ; `.env` được Git bỏ qua và `.env.example` không chứa secret thật.
- [x] Sau khi sửa sang Java 17, `.\mvnw.cmd --no-transfer-progress clean verify` chạy thành công trên Temurin Java 25 (main/test compile với `release 17`; 2 test pass).
- [x] `docker compose config --quiet` chạy thành công.
- [x] Docker image Java 17 build được; ứng dụng và database khởi động thành công (`docker compose up -d --build`).
- [x] Readiness health báo cả ứng dụng và database khỏe (`status=UP`, `components.db.status=UP`); `java -version` trong container báo Temurin 17.0.20.1.
- [x] Thay đổi chỉ nằm trong repository BookNest.
- [x] Đã xóa các image build/runtime Java 21 được gắn tag riêng và output sinh tự động trong `target/` sau khi đổi baseline; không prune tài nguyên Docker khác.

Ban đầu Docker Hub từ chối tải image do lỗi xác thực cục bộ. Các tag image chính thức tương ứng được tải từ public Amazon ECR mirror và gắn tag trong Docker cache local để xác minh; file cấu hình dự án vẫn dùng tên image chính thức thông thường.

### Danh sách file Phase 0

Các file sau được tạo hoặc chỉnh sửa trong Phase 0. Danh sách này được ghi lại sau khi làm xong; checklist Phase 0 cũng được tạo sau khi bắt đầu triển khai, chưa đúng quy trình người dùng yêu cầu. Với mọi phase sau, phải chuẩn bị checklist trước và cập nhật danh sách này sau khi hoàn tất từng file.

- [x] `pom.xml` — dependency Spring Boot và cấu hình build Java 17.
- [x] `.gitignore` — bỏ qua build artifact và file môi trường local.
- [x] `.gitattributes` — quy tắc line ending.
- [x] `.env.example` — cấu hình mẫu local-only cho Compose.
- [x] `.mvn/wrapper/maven-wrapper.properties` — cấu hình phân phối Maven Wrapper.
- [x] `mvnw` và `mvnw.cmd` — script chạy Maven Wrapper cho Unix và Windows.
- [x] `src/main/java/com/booknest/BookNestApplication.java` — entry point Spring Boot.
- [x] `src/main/resources/application.yml` — cấu hình PostgreSQL và health.
- [x] `src/test/java/com/booknest/BookNestApplicationTests.java` — smoke test khởi tạo context và readiness.
- [x] `src/test/resources/application-test.yml` — profile test riêng dùng H2.
- [x] `Dockerfile` — image multi-stage Java 17 và runtime non-root.
- [x] `compose.yaml` — app/database, health check, loopback binding và volume riêng.
- [x] `README.md` — phạm vi Phase 0 và hướng dẫn chạy/test local.
- [x] `AGENTS.md` — bối cảnh dự án dùng chung, trạng thái phase và quy tắc cho agent.
- [x] `PHASE_CHECKLIST.md` — tiêu chí nghiệm thu và danh sách file.
- [x] `PHASE_CHECKLIST.vi.md` — bản tiếng Việt của checklist phase.
- [x] `pom.xml`, `Dockerfile`, `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — đổi baseline từ Java 21 sang Java 17 theo xác nhận của người dùng; đã chạy lại Maven test và build/runtime Docker Java 17 thành công.

## Các phase tiếp theo

### Phase 1 — Đăng nhập và cách ly dữ liệu theo tài khoản (đã chuẩn bị; chưa triển khai)

#### Tiêu chí nghiệm thu

- [ ] Thống nhất và ghi rõ mỗi tài khoản đăng ký tự sở hữu đúng một thư viện cá nhân, hay tài khoản và thư viện được biểu diễn bằng một mô hình sở hữu theo tài khoản.
- [ ] Đăng ký tạo tài khoản với username duy nhất và mật khẩu BCrypt; không bao giờ trả mật khẩu trong response.
- [ ] Đăng nhập/đăng xuất dùng session phía server và cookie qua Spring Security; không dùng JWT hoặc tự viết cơ chế xác thực.
- [ ] Giữ CSRF protection bật; client chưa đăng nhập có thể lấy token và API từ chối request thay đổi dữ liệu nếu thiếu token hợp lệ.
- [ ] Request chưa xác thực không thể truy cập endpoint nghiệp vụ được bảo vệ.
- [ ] Danh tính tài khoản hiện tại lấy từ Spring Security, không lấy từ user/owner/library ID do client gửi.
- [ ] Query dữ liệu theo tài khoản được giới hạn nhất quán; test chứng minh tài khoản A không đọc/sửa/xóa được dữ liệu tài khoản B.
- [ ] Ràng buộc schema và response lỗi liên quan được triển khai và kiểm thử.
- [ ] `.\mvnw.cmd --no-transfer-progress clean verify` thành công; ghi nhận kiểm tra tích hợp Docker/PostgreSQL nếu có chạy.
- [ ] Danh sách file bên dưới được cập nhật sau khi hoàn tất từng file thực tế.

#### Danh sách file dự kiến (điều chỉnh sau khi chốt thiết kế, trước khi code)

- [ ] `pom.xml` — thêm dependency Spring Security nếu chưa có.
- [ ] `src/main/java/com/booknest/...` — model lưu tài khoản/thư viện, repository, service, DTO và API; cấu trúc theo dự án thực tế.
- [ ] `src/main/java/com/booknest/...` — cấu hình Spring Security session, CSRF, password encoder và current-account.
- [ ] `src/main/resources/application.yml` — chỉ cập nhật cấu hình security/session khi cần; không thêm secret.
- [ ] `src/test/java/com/booknest/...` — test xác thực, CSRF và cách ly chéo tài khoản.
- [ ] `src/test/resources/application-test.yml` — cập nhật cấu hình test nếu thực sự cần.
- [ ] `PHASE_CHECKLIST.md` — cập nhật checklist tiếng Anh và danh sách file sau khi hoàn tất từng file.
- [ ] `PHASE_CHECKLIST.vi.md` — cập nhật checklist tiếng Việt tương ứng sau khi hoàn tất từng file.
- [ ] `README.md` — ghi lại hành vi Phase 1 đã xác minh và hướng dẫn local liên quan.

**Trạng thái:** Checklist đã chuẩn bị trước khi triển khai Phase 1. Phase 1 chưa bắt đầu và cần người dùng yêu cầu rõ.
