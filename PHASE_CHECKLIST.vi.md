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

### Phase 1 — Đăng nhập và truy cập thư viện dùng chung (đã cập nhật checklist trước khi triển khai)

#### Tiêu chí nghiệm thu

- [x] Mô hình sản phẩm đã chốt: mỗi lần chạy ứng dụng/database có một thư viện dùng chung; tài khoản đăng nhập là nhân viên, `Member` là người mượn, mọi tài khoản đăng ký có cùng quyền cơ bản.
- [x] Đăng ký tạo tài khoản nhân viên với username duy nhất và mật khẩu BCrypt; không bao giờ trả mật khẩu/hash trong response.
- [x] Đăng nhập/đăng xuất dùng session phía server và cookie qua Spring Security; không dùng JWT hoặc tự viết cơ chế xác thực.
- [x] CSRF protection luôn bật; trình duyệt lấy được token trước khi đăng nhập và request thay đổi dữ liệu thiếu token hợp lệ bị từ chối.
- [x] Request chưa xác thực không truy cập được API cần đăng nhập; chỉ cho phép đăng ký, đăng nhập, bootstrap CSRF, tài nguyên tĩnh và health endpoint an toàn.
- [x] Danh tính nhân viên hiện tại lấy từ Spring Security, không lấy từ actor/account ID do client gửi.
- [x] Mọi tài khoản đăng ký có cùng authority cơ bản; không tạo phân cấp role/quyền.
- [x] Không tạo `Library` riêng cho từng tài khoản và không gắn quyền sở hữu tài khoản lên sách, bản sách, thành viên, lượt mượn, đặt trước hoặc lịch sử dùng chung.
- [x] Không cấp kho riêng cho từng tài khoản; test xác minh các tài khoản đăng ký độc lập có cùng context/quyền. Test nhiều tài khoản cùng nhìn thấy tồn kho và trạng thái sách sẽ làm cùng nghiệp vụ ở Phase 2.
- [x] Username được đảm bảo duy nhất ở cả logic ứng dụng và database; lỗi liên quan có response an toàn và được kiểm thử.
- [x] `.\mvnw.cmd --no-transfer-progress clean verify` thành công: 10 test, 0 lỗi/thất bại/bỏ qua. `docker compose config --quiet` và `docker compose up -d --build` thành công; PostgreSQL 17.11 chạy Flyway V1, app và DB khỏe, readiness UP, CSRF/cookie session đạt, anonymous gọi `/api/auth/me` nhận 401, và luồng đăng ký/đăng nhập/me/đăng xuất thử trả 201/200/200/204. Đã xóa tài khoản thử sau kiểm tra.
- [x] Đã cập nhật đồng bộ danh sách file ở cả hai bản checklist khi hoàn tất từng file.

#### Danh sách file dự kiến (cập nhật trước khi thêm file ngoài dự kiến)

- [x] `pom.xml` — thêm Spring Security, Flyway starter/PostgreSQL và dependency test Spring Security.
- [x] `src/main/java/com/booknest/account/StaffAccount.java` — danh tính đăng nhập, không sở hữu bản ghi thư viện chung.
- [x] `src/main/java/com/booknest/account/StaffAccountRepository.java` — tra cứu username và lưu với unique constraint.
- [x] `src/main/java/com/booknest/account/RegistrationRequest.java` — validate username/password đầu vào.
- [x] `src/main/java/com/booknest/account/StaffAccountResponse.java` — chỉ trả account ID và username.
- [x] `src/main/java/com/booknest/account/StaffAccountAlreadyExistsException.java` — biểu diễn trường hợp username sau chuẩn hóa đã tồn tại.
- [x] `src/main/java/com/booknest/account/AccountService.java` — đăng ký, username chuẩn hóa/duy nhất, tra cứu account và mã hóa BCrypt.
- [x] `src/main/java/com/booknest/account/AccountController.java` — đăng ký, nhân viên hiện tại và bootstrap CSRF; Spring Security filter xử lý đăng nhập/đăng xuất.
- [x] `src/main/java/com/booknest/web/ApiExceptionHandler.java` — response validation/conflict an toàn; được mở rộng ở Phase 2 cho JSON sai và lỗi không tìm thấy/xung đột nghiệp vụ.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java` — xác thực session, CSRF, route public/protected và quyền chung của nhân viên.
- [x] `src/main/resources/db/migration/V1__create_staff_accounts.sql` — tạo bảng tài khoản nhân viên dùng chung, username unique được đảm bảo ở database.
- [x] `src/main/resources/application.yml` — cấu hình session cookie HTTP-only, same-site và thời hạn; không thêm secret.
- [x] `src/test/java/com/booknest/account/AccountControllerTests.java` — test đăng ký, username trùng, BCrypt trong DB, response an toàn và quyền nhân viên tương đương.
- [x] `src/test/java/com/booknest/security/SecurityConfigurationTests.java` — test session login/logout, CSRF và từ chối anonymous.
- [x] `src/test/resources/application-test.yml` — dùng migration chung và Hibernate validation trong profile H2 test.
- [x] `AGENTS.md` — ghi nhận mô hình thư viện dùng chung và trạng thái/ủy quyền Phase 1.
- [x] `README.md` — mô tả thư viện chung, tài khoản nhân viên và hồ sơ người mượn `Member`.
- [x] `PHASE_CHECKLIST.md` — thay tiêu chí tenant-isolation không còn phù hợp và chuẩn bị ledger Phase 1.
- [x] `PHASE_CHECKLIST.vi.md` — đồng bộ checklist Phase 1 tiếng Việt với bản tiếng Anh.
- [x] `README.md` — ghi API Phase 1 đã kiểm chứng, giới hạn hiện tại và cách chạy/test.

**Trạng thái:** Đã hoàn tất triển khai, kiểm chứng và được người dùng nghiệm thu Phase 1. Mô hình thư viện dùng chung, quyền nhân viên như nhau và `Member` là người mượn đã được chốt trước khi triển khai. Phase 2 được bắt đầu sau yêu cầu rõ ràng của người dùng và đã được nghiệm thu.

### Phase 2 — Đầu sách và từng bản sách

Người dùng đã xem và duyệt phạm vi này trước khi triển khai Phase 2. Các mục bên dưới ghi lại kết quả kiểm chứng triển khai và nghiệm thu.

#### Phạm vi đã duyệt

- Quản lý đầu sách và từng bản sách vật lý riêng biệt trong một thư viện dùng chung.
- Mọi tài khoản nhân viên đã đăng nhập có thể thao tác cùng kho; không có trường sở hữu theo tài khoản hoặc bộ lọc theo tài khoản.
- Đầu sách lưu thông tin thư mục; mỗi bản sách vật lý là một bản ghi riêng, liên kết đúng một đầu sách.
- Trường đầu sách đề xuất ban đầu: tên sách và tác giả bắt buộc; ISBN, thể loại, năm xuất bản và mô tả không bắt buộc. Có validation hợp lý về độ dài/giá trị và chuẩn hóa ISBN tùy chọn trước khi kiểm tra duy nhất.
- Trạng thái bản sách đề xuất: `AVAILABLE`, `MAINTENANCE`, `RETIRED`. Chỉ bản `AVAILABLE` được tính là sẵn sàng cho mượn; trạng thái mượn/đặt trước để phase sau xử lý.
- Không xóa đầu sách nếu vẫn còn bản sách thuộc đầu sách đó; không cascade xóa kho. Chỉ xóa bản sách khi không vi phạm tham chiếu hoặc quy tắc tồn kho.
- Tìm kiếm, lọc, phân trang/sắp xếp ở database, thành viên, mượn-trả, đặt trước, dashboard và giao diện không nằm trong phase này.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã xem và duyệt các trường đầu sách, trạng thái bản sách và quy tắc xóa đề xuất trước khi triển khai.
- [x] Nhân viên đã đăng nhập tạo, xem danh sách/chi tiết, cập nhật và xóa an toàn đầu sách; lỗi validation và không tìm thấy dữ liệu có response API an toàn, nhất quán.
- [x] Nhân viên đã đăng nhập thêm, xem danh sách, đổi trạng thái và xóa an toàn từng bản sách thuộc đầu sách; bản sách không thể tham chiếu đầu sách không tồn tại.
- [x] Database đảm bảo trường bắt buộc, toàn vẹn khóa ngoại và ISBN khác null là duy nhất.
- [x] Số bản có thể cho mượn được tính nhất quán từ bản ghi/trạng thái bản sách; chỉ trạng thái `AVAILABLE` được tính.
- [x] Tài khoản nhân viên thứ hai nhìn thấy đầu sách, bản sách, thay đổi trạng thái và tồn kho do tài khoản đầu tạo; không tạo kho riêng theo tài khoản.
- [x] Người chưa đăng nhập không thể đọc hoặc sửa API nghiệp vụ đầu sách/bản sách; request ghi dữ liệu vẫn cần CSRF protection hiện có.
- [x] Test bao phủ thao tác thành công, validation, ISBN trùng, tham chiếu không tồn tại, trạng thái không hợp lệ, giới hạn xóa, authorization và dữ liệu kho dùng chung.
- [x] Hành vi Phase 0/1 hiện có không bị ảnh hưởng; `.\mvnw.cmd --no-transfer-progress clean verify` thành công (16 test, 0 lỗi/thất bại/bỏ qua).
- [x] Migration V2 và ứng dụng được kiểm tra với PostgreSQL Docker Compose hiện có mà không xóa/reset volume; kiểm tra thực tế xác nhận hai tài khoản cùng thấy đầu sách/bản sách/trạng thái/số lượng.
- [x] Duy trì hai danh sách file song ngữ đồng bộ và cập nhật khi hoàn tất từng file.

#### Danh sách file dự kiến (cập nhật cả hai checklist trước khi thêm file ngoài kế hoạch)

- [x] `src/main/java/com/booknest/book/Book.java` — entity đầu sách dùng chung và các trường đã xác thực.
- [x] `src/main/java/com/booknest/book/BookCopy.java` — bản sách vật lý, quan hệ đầu sách và trạng thái.
- [x] `src/main/java/com/booknest/book/BookRepository.java` — lưu trữ và tra cứu đầu sách.
- [x] `src/main/java/com/booknest/book/BookCopyRepository.java` — lưu trữ bản sách và truy vấn số bản có thể cho mượn.
- [x] `src/main/java/com/booknest/book/BookRequest.java` và `BookResponse.java` — dữ liệu đầu vào đã validate và response an toàn, gồm số bản sách tính toán.
- [x] `src/main/java/com/booknest/book/BookCopyRequest.java` và `BookCopyResponse.java` — thao tác bản sách đã validate và response an toàn.
- [x] `src/main/java/com/booknest/book/BookService.java` — nghiệp vụ đầu sách, chuẩn hóa/duy nhất ISBN, thống kê tồn kho và xóa an toàn.
- [x] `src/main/java/com/booknest/book/BookCopyService.java` — nghiệp vụ bản sách, đổi trạng thái và kiểm tra quan hệ/đầu sách.
- [x] `src/main/java/com/booknest/book/BookController.java` và `BookCopyController.java` — REST endpoint được bảo vệ bởi quy tắc đăng nhập `/api/**` hiện có.
- [x] `src/main/resources/db/migration/V2__create_books_and_copies.sql` — schema, khóa ngoại, check năm/trạng thái, ISBN duy nhất và index tra cứu bản sách.
- [x] `src/test/java/com/booknest/book/BookControllerTests.java` và `BookCopyControllerTests.java` — test API, validation, access, chuẩn hóa ISBN, giới hạn xóa, số bản và kho dùng chung giữa nhân viên.
- [x] `README.md` — ghi endpoint Phase 2, trạng thái bản sách, quy tắc xóa và phạm vi hiện tại.
- [x] `AGENTS.md` — ghi quyết định Phase 2, trạng thái triển khai, kiểm chứng và phần còn lại.
- [x] `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — giữ phạm vi đã duyệt, kết quả nghiệm thu và ledger từng file đồng bộ.

**Trạng thái:** Đã hoàn tất triển khai, kiểm chứng và được người dùng nghiệm thu Phase 2.

### Phần giao diện bổ sung — Đăng nhập và sách/bản sách

Phần giao diện này thực hiện theo quyết định của người dùng: làm UI từng phần sau khi backend tương ứng được nghiệm thu. Người dùng đã duyệt phạm vi trước khi triển khai. Các mục bên dưới ghi lại kết quả kiểm chứng và nghiệm thu sau đó.

#### Phạm vi dự kiến

- Dùng ứng dụng Spring Boot cùng origin hiện có và HTML, CSS, JavaScript thuần; không thêm framework frontend hay server riêng.
- Có giao diện đăng ký/đăng nhập/đăng xuất tối thiểu để nhân viên tạo session và sử dụng màn hình sách/bản sách được bảo vệ.
- Có màn hình kho sách dùng chung, danh sách cùng tổng số bản/số bản còn sẵn sàng; tạo, sửa, xem chi tiết và xóa đầu sách theo quy tắc hiện có là không được xóa khi còn bản sách.
- Có quản lý bản sách theo đầu sách đang chọn: xem danh sách, thêm bản, đổi trạng thái và xóa bản.
- Lấy CSRF token từ endpoint hiện có và gửi kèm mọi request làm thay đổi dữ liệu; dùng cookie session HTTP-only, JavaScript không đọc cookie.
- Chỉ làm UI xác thực và sách/bản sách. Thành viên, mượn-trả, đặt trước, dashboard, tìm kiếm/lọc/phân trang nâng cao và giao diện tổng thể hoàn chỉnh để các phần sau.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã xem và duyệt phạm vi giao diện trình duyệt đề xuất trước khi triển khai.
- [x] Đường dẫn gốc ứng dụng phục vụ được trang BookNest dùng được, responsive từ Spring Boot cùng origin.
- [x] Nhân viên đăng ký, đăng nhập, xem danh tính hiện tại và đăng xuất qua UI; lỗi dễ hiểu và mật khẩu không được lưu trong browser storage.
- [x] Nhân viên đã đăng nhập xem danh sách/chi tiết đầu sách và số bản dùng chung; tạo/sửa/xóa đầu sách qua form có validation và trạng thái thành công/lỗi rõ ràng.
- [x] Nhân viên đã đăng nhập xem/thêm/cập nhật/xóa bản sách theo đầu sách; nhãn trạng thái dễ hiểu và số lượng còn sẵn sàng cập nhật sau thao tác.
- [x] Bootstrap CSRF/session hoạt động qua đăng nhập/đăng xuất; request API thay đổi dữ liệu gửi đúng header/token; UI không làm lộ nghiệp vụ cho người chưa đăng nhập.
- [x] Nội dung sách/member không đáng tin cậy được hiển thị qua DOM API an toàn như `textContent`; trình duyệt xác nhận title/description giống mã độc vẫn là văn bản, không tạo element được chèn.
- [x] Có trạng thái loading, danh sách trống, validation, lỗi mạng/API, thành công và xác nhận hành động phá hủy; không có nút giả hoặc thao tác placeholder.
- [x] Màn hình chính sử dụng được trên desktop/điện thoại và có label/control hỗ trợ bàn phím; viewport 375px không bị tràn ngang trang.
- [x] Backend hiện tại không bị ảnh hưởng; `.\mvnw.cmd --no-transfer-progress clean verify` thành công (16 test) và đã thử thủ công luồng đăng ký/đăng nhập/sách/bản sách/trạng thái/xóa/đăng xuất trên Compose.
- [x] Duy trì đồng bộ hai checklist UI và ledger từng file khi hoàn tất từng file.

#### Danh sách file dự kiến (cập nhật cả hai checklist trước khi thêm file ngoài kế hoạch)

- [x] `src/main/resources/static/index.html` — khung trang/landmark accessible, form xác thực và vùng validation, dialog có label, màn hình chi tiết đầu sách/bản sách và form.
- [x] `src/main/resources/static/css/styles.css` — style responsive, chỉ báo trạng thái và focus state bằng font hệ thống cục bộ.
- [x] `src/main/resources/static/js/api.js` — request cùng origin, bootstrap/header CSRF và xử lý lỗi API an toàn.
- [x] `src/main/resources/static/js/auth.js` — đăng ký/đăng nhập/đăng xuất và UI phiên hiện tại.
- [x] `src/main/resources/static/js/books.js` — tải đầu sách/bản sách, xem chi tiết, form, đổi trạng thái, xác nhận và render DOM an toàn.
- [x] `src/main/resources/static/js/app.js` — khởi tạo hành vi, phối hợp module và truyền callback thông báo cho xác thực.
- [x] `README.md` — ghi địa chỉ trình duyệt, đăng ký lần đầu, phạm vi UI đã kiểm chứng và giới hạn hiện tại.
- [x] `AGENTS.md` — ghi phạm vi UI được duyệt, file, kiểm chứng test/thủ công và checkpoint.
- [x] `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — đồng bộ phạm vi, kết quả và danh sách file giao diện.

**Trạng thái:** Phần UI đăng nhập/sách/bản sách đã được triển khai, kiểm chứng và người dùng nghiệm thu. Không bắt đầu phần UI/backend tiếp theo cho đến khi người dùng yêu cầu tiếp tục một cách rõ ràng.

### Phase 3 — Thành viên (đã duyệt; đã kiểm chứng triển khai)

Đây là mục tiếp theo trong roadmap, tập trung vào backend. Không làm giao diện quản lý thành viên trong phase này; sau khi API thành viên được triển khai và nghiệm thu, hãy chuẩn bị một checklist song ngữ riêng cho phần UI trước khi viết code.

#### Phạm vi đã duyệt

- Quản lý hồ sơ thành viên/người mượn trong cùng một thư viện dùng chung; thành viên không phải tài khoản đăng nhập.
- Mỗi thành viên có họ tên bắt buộc; email, số điện thoại và ghi chú không bắt buộc. Không bắt buộc email/số điện thoại duy nhất vì thông tin liên hệ có thể được dùng chung.
- Cung cấp API yêu cầu đăng nhập để tạo, xem danh sách, xem chi tiết, cập nhật và xóa thành viên. Chuẩn hóa khoảng trắng đầu/cuối và chuyển trường tùy chọn để trống thành null.
- Chỉ cho xóa hẳn thành viên khi chưa được tham chiếu bởi lượt mượn, đặt trước hoặc lịch sử hoạt động. Không cascade xóa hoặc làm mất lịch sử mượn. Vì các bản ghi tham chiếu này sẽ được tạo ở phase sau, cần định nghĩa và kiểm thử hành vi xóa khi bổ sung các quan hệ đó; dùng khóa ngoại chặn xóa và response conflict an toàn.
- Chưa làm tìm kiếm/lọc/phân trang, nghiệp vụ mượn, đặt trước, màn hình lịch sử và UI thành viên trong backend phase này.
- Dùng stack Java 17/Spring Boot/PostgreSQL/Flyway/Maven hiện tại, session authentication, CSRF, validation, xử lý lỗi và mô hình thư viện dùng chung. Không thêm framework hay dịch vụ mới.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã xem và duyệt các trường thành viên, cách xử lý thông tin liên hệ tùy chọn và quy tắc xóa/lịch sử trước khi triển khai.
- [x] Nhân viên đã đăng nhập có thể tạo, xem danh sách/chi tiết, cập nhật và xóa thành viên chưa được tham chiếu qua REST API có tài liệu; test xác nhận mọi tài khoản nhân viên nhìn thấy cùng bản ghi.
- [x] Họ tên bắt buộc và có giới hạn độ dài; email, số điện thoại, ghi chú là tùy chọn, có validation và giới hạn độ dài. Khoảng trắng được chuẩn hóa; giá trị tùy chọn chỉ chứa khoảng trắng được lưu/trả về là null.
- [x] Database đảm bảo trường bắt buộc/độ dài; dữ liệu thành viên không chứa thông tin đăng nhập nhân viên hoặc quyền sở hữu tài khoản.
- [x] Request chưa đăng nhập không dùng được API thành viên; request ghi dữ liệu yêu cầu CSRF.
- [x] Input không hợp lệ và ID thành viên không tồn tại được xử lý với response API an toàn, nhất quán, đúng status code.
- [ ] Khi thành viên được tham chiếu bởi lượt mượn, đặt trước hoặc bản ghi lịch sử, thao tác xóa phải trả conflict an toàn và giữ nguyên lịch sử liên quan. Kiểm tra khi triển khai các quan hệ đó.
- [ ] Xóa thành viên không bao giờ cascade hoặc phá hủy lượt mượn, đặt trước hay lịch sử hoạt động. Các phase tạo quan hệ sau này phải dùng khóa ngoại chặn xóa và từ chối xóa với conflict; kiểm tra quy tắc này khi triển khai các phase tham chiếu thành viên.
- [x] Test bao phủ CRUD, validation/chuẩn hóa, not-found, authorization/CSRF, dữ liệu dùng chung giữa các tài khoản và xóa thành viên chưa được tham chiếu. Hành vi thành viên đã được tham chiếu sẽ kiểm tra sau khi có schema mượn/lịch sử.
- [x] Hành vi Phase 0–2 và UI tài khoản/sách hiện có không bị ảnh hưởng; `.\mvnw.cmd --no-transfer-progress clean verify` thành công (20 test, 0 lỗi/thất bại/bỏ qua).
- [x] Flyway V3 và CRUD/chuẩn hóa thành viên thực tế được kiểm tra với PostgreSQL Compose hiện có mà không xóa/reset volume; app và DB khỏe, readiness UP.
- [x] Hai checklist song ngữ và danh sách file dự kiến được giữ đồng bộ.

**Ghi chú kiểm chứng:** Hiện chưa có lượt mượn, đặt trước hay lịch sử hoạt động nên chưa có quan hệ tham chiếu thành viên để thử. Có thể xóa cứng thành viên chưa được tham chiếu. Khi thêm các quan hệ ở phase sau, phải giữ bản ghi thành viên và từ chối xóa bằng khóa ngoại chặn xóa; trường hợp này sẽ được kiểm tra sau.

#### Danh sách file dự kiến (cập nhật cả hai checklist trước khi thêm file ngoài kế hoạch)

- [x] `src/main/java/com/booknest/member/Member.java` — entity người mượn dùng chung, gồm thông tin thành viên/liên hệ đã duyệt.
- [x] `src/main/java/com/booknest/member/MemberRepository.java` — lưu trữ và truy vấn danh sách/chi tiết có thứ tự ổn định.
- [x] `src/main/java/com/booknest/member/MemberRequest.java` và `MemberResponse.java` — input được validate và API response an toàn.
- [x] `src/main/java/com/booknest/member/MemberService.java` — CRUD, chuẩn hóa và quy tắc xóa an toàn.
- [x] `src/main/java/com/booknest/member/MemberController.java` — REST endpoint yêu cầu đăng nhập cho thành viên.
- [x] `src/main/resources/db/migration/V3__create_members.sql` — schema và ràng buộc database cho thành viên.
- [x] `src/test/java/com/booknest/member/MemberControllerTests.java` — test API, validation, authorization, dữ liệu dùng chung và chính sách xóa.
- [x] `README.md` — tài liệu trường thành viên, endpoint API, quy tắc xóa/lịch sử và phạm vi hiện tại.
- [x] `AGENTS.md` — ghi quyết định thành viên đã duyệt, trạng thái triển khai, kiểm chứng và phần còn lại.
- [x] `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — đồng bộ kết quả nghiệm thu và danh sách file.

**Trạng thái:** Backend thành viên đã triển khai, kiểm chứng và được người dùng nghiệm thu. Trường hợp xóa thành viên đã được tham chiếu sẽ kiểm tra khi các phase mượn/đặt trước/lịch sử bổ sung quan hệ. UI thành viên là lát cắt riêng; cần có checklist song ngữ được duyệt trước khi triển khai.

### Lát cắt UI bổ sung — Thành viên (đã duyệt; đã kiểm chứng triển khai)

Phần này bổ sung giao diện trình duyệt cho API thành viên đã được nghiệm thu. Không chỉnh sửa UI cho đến khi người dùng duyệt checklist.

#### Phạm vi đề xuất

- Mở rộng workspace sau đăng nhập với điều hướng đơn giản **Kho sách / Thành viên**; giữ nguyên UI và hành vi sách/bản sách hiện tại.
- Cung cấp danh sách thành viên dùng chung và luồng tạo/sửa/xóa bằng endpoint `/api/members` hiện có cùng helper `apiRequest` cùng origin.
- Form thành viên có họ tên bắt buộc, email, số điện thoại và ghi chú không bắt buộc; giới hạn phía trình duyệt khớp backend, giải thích rõ trường liên hệ tùy chọn/để trống.
- Hiển thị mọi nội dung do thành viên nhập bằng DOM API an toàn và `textContent`. Có trạng thái đang tải, danh sách trống, validation, lỗi API/mạng, thành công và xác nhận xóa.
- Giữ session-cookie/CSRF hiện tại; không đọc cookie HTTP-only hoặc lưu dữ liệu thành viên/mật khẩu trong browser storage.
- Chưa làm tìm kiếm, lọc, phân trang, lượt mượn, đặt trước, lịch sử và dashboard trong lát cắt UI này.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã xem và duyệt phạm vi UI thành viên trước khi triển khai.
- [x] Nhân viên đã đăng nhập chuyển được giữa Kho sách và Thành viên mà không làm hỏng/ẩn luồng sách/bản sách hiện tại; màn hình thành viên dùng được trên desktop và điện thoại.
- [x] Nhân viên xem danh sách thành viên dùng chung, tạo/cập nhật với họ tên bắt buộc và thông tin liên hệ/ghi chú tùy chọn, xóa thành viên chưa được tham chiếu qua giao diện.
- [x] UI chuẩn hóa giá trị nhất quán với API và coi trường tùy chọn trống là null; validation trình duyệt và lỗi API dễ hiểu.
- [x] Xóa cần xác nhận; UI hiển thị an toàn response conflict 409. Trường hợp conflict thực tế khi có lượt mượn/lịch sử sẽ kiểm tra sau khi các quan hệ backend được bổ sung.
- [x] Nội dung tên/liên hệ/ghi chú không đáng tin cậy được hiển thị dạng text, không phải HTML; tính năng không dùng `innerHTML`, `outerHTML` hay browser storage. Kiểm tra DOM không thấy element `img`/`script` được chèn.
- [x] Có trạng thái loading, empty, validation, success, error và xác nhận hành động phá hủy; control có nhãn và dùng được bằng bàn phím.
- [x] Luồng auth và sách/bản sách hiện có vẫn hoạt động; kiểm tra cú pháp JavaScript, `.\mvnw.cmd --no-transfer-progress clean verify` (20 test) và test UI thủ công trên Compose đều thành công.
- [x] Kiểm tra trình duyệt bao gồm CRUD thành viên, email sai/trường tùy chọn trống, hiển thị API 409 mô phỏng, render an toàn và viewport 375px không tràn ngang tài liệu (độ rộng tài liệu 360px).
- [x] Hai checklist song ngữ và danh sách file được giữ đồng bộ.

#### Danh sách file dự kiến (cập nhật cả hai checklist trước khi thêm file ngoài kế hoạch)

- [x] `src/main/resources/static/index.html` — thêm điều hướng workspace accessible và vùng danh sách/form/dialog/feedback thành viên, giữ nguyên markup sách.
- [x] `src/main/resources/static/css/styles.css` — style điều hướng, danh sách và form thành viên responsive theo design system hiện tại.
- [x] `src/main/resources/static/js/members.js` — tải/render dữ liệu và luồng tạo/sửa/xóa bằng DOM API an toàn.
- [x] `src/main/resources/static/js/app.js` — khởi tạo module thành viên, kết nối đổi view/vòng đời xác thực.
- [x] `src/main/resources/static/js/api.js` — dịch thông báo API không tìm thấy thành viên và dùng thông báo conflict chung an toàn.
- [x] `README.md` — mô tả UI thành viên và cập nhật phạm vi giao diện hiện tại.
- [x] `AGENTS.md` — ghi phạm vi UI đã duyệt, triển khai, kiểm chứng và phần còn lại.
- [x] `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — đồng bộ tiêu chí UI thành viên và danh sách file.

**Ghi chú kiểm chứng:** Chưa có quan hệ lượt mượn/lịch sử nên thông báo conflict được kiểm tra bằng response 409 mô phỏng. Quy tắc chặn xóa thành viên đã được tham chiếu trong database sẽ kiểm thử ở phase backend tương ứng.

**Trạng thái:** Lát cắt UI thành viên đã triển khai, kiểm chứng theo phạm vi được duyệt và được người dùng nghiệm thu. Chưa bắt đầu phase backend/UI kế tiếp cho đến khi người dùng yêu cầu rõ ràng.

### Phase 4 — Mượn và trả sách (đã kiểm chứng triển khai; chờ người dùng nghiệm thu)

Phase backend này triển khai nghiệp vụ mượn và trả sách. Người dùng đã duyệt hạn mượn mặc định 14 ngày dương lịch tính từ ngày mượn và toàn bộ phạm vi trước khi bắt đầu.

#### Phạm vi đề xuất

- Một lượt mượn liên kết một thành viên dùng chung với đúng một bản sách vật lý. API mượn nhận ID thành viên và ID bản sách; danh tính nhân viên thao tác lấy từ session đã xác thực, không nhận từ request.
- Lưu ngày mượn, hạn trả, ngày trả, nhân viên lập phiếu và nhân viên nhận trả trong bản ghi lượt mượn. Giữ bản ghi lượt đã trả làm lịch sử; trạng thái đang mượn/quá hạn được suy ra từ ngày trả và hạn trả, không lưu một cờ trạng thái có thể cũ.
- Theo chính sách người dùng đã chọn: ngày mượn là ngày nghiệp vụ hiện tại của thư viện, hạn trả sau 14 ngày dương lịch. Request không được ghi đè hạn trả. Khi trả, lưu ngày nghiệp vụ hiện tại; quá hạn nghĩa là chưa trả và hạn trả trước ngày nghiệp vụ hiện tại.
- Mỗi bản sách có tối đa một lượt mượn đang hoạt động. Khi mượn, đổi trạng thái `AVAILABLE` thành `ON_LOAN`; khi trả, đổi lại `AVAILABLE`. Từ chối bản sách không sẵn sàng, không tồn tại, đã ngừng sử dụng/đang bảo trì; từ chối thành viên không tồn tại, mượn trùng và trả lặp.
- Tuần tự hóa các yêu cầu mượn cùng một bản sách bằng transaction và khóa dòng database (hoặc chiến lược atomic tương đương đáng tin cậy), để hai request tranh bản cuối không cùng thành công.
- Bảo toàn thành viên, bản sách và lượt mượn bằng khóa ngoại chặn xóa. Thành viên có lịch sử mượn và bản sách có lịch sử mượn không được xóa; không cascade xóa. Chuyển lỗi xóa thành viên đang được tham chiếu thành API error an toàn, dễ hiểu.
- Cung cấp API mượn/trả yêu cầu đăng nhập và CSRF, response an toàn cho validation/not-found/conflict cùng test.
- Chưa làm gia hạn, đặt trước/hàng chờ/giữ chỗ, API lịch sử/tìm kiếm, phân trang, dashboard hoặc UI mượn/thành viên trong backend phase này.
- Dùng pattern Java 17/Spring Boot/PostgreSQL/Flyway/Maven, session/CSRF, API error và thư viện dùng chung hiện có; không thêm framework hay dịch vụ mới.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã chọn hạn mượn mặc định cố định 14 ngày dương lịch từ ngày mượn; client không được chọn/ghi đè hạn trả.
- [x] Người dùng đã xem và duyệt các trường lượt mượn/quy thuộc nhân viên thao tác, chuyển trạng thái bản sách, hành vi trả và quy tắc xóa bản ghi đã được tham chiếu trước khi triển khai.
- [x] Nhân viên đã đăng nhập mượn được bản sách sẵn sàng cho thành viên tồn tại và trả được lượt mượn đang hoạt động; mọi tài khoản nhân viên nhìn thấy cùng lượt mượn đang mở và trạng thái tồn kho cập nhật.
- [x] Ngày mượn/ngày trả và ID nhân viên thao tác được lấy ở server từ authentication context. Lượt đã trả được giữ lại làm lịch sử.
- [x] Hạn trả bằng ngày nghiệp vụ cộng 14 ngày; trạng thái quá hạn được suy ra từ hạn trả của lượt mượn chưa trả, không lưu riêng.
- [x] Mỗi bản sách chỉ có tối đa một lượt mượn đang hoạt động. Từ chối thành viên/bản sách không tồn tại và bản sách không `AVAILABLE`; từ chối trả lượt không tồn tại/đã trả.
- [x] Chuyển trạng thái bản sách nhất quán (`AVAILABLE` → `ON_LOAN` → `AVAILABLE`); nhân viên không thể tự đổi bản đang mượn thành sẵn sàng hoặc xóa bản có lịch sử mượn.
- [x] Các yêu cầu mượn đồng thời cho cùng bản sách cuối được tuần tự hóa; chỉ tối đa một request thành công. Test H2 và kiểm tra API đồng thời trên PostgreSQL Compose đều cho đúng một thành công và một conflict.
- [x] Khóa ngoại chặn xóa thành viên/bản sách có lịch sử mượn; response conflict với thành viên an toàn và không có lượt mượn/lịch sử nào bị cascade xóa.
- [x] Request anonymous bị từ chối và API ghi dữ liệu cần CSRF; response validation/not-found/business-conflict/request sai an toàn, nhất quán.
- [x] Test bao phủ luồng mượn/trả, ngày hạn/quá hạn, chia sẻ dữ liệu, tham chiếu sai/không tồn tại, trả lặp, trạng thái bản sách, xóa bản ghi được tham chiếu, authorization và CSRF.
- [x] Backend Phase 0–3 và UI đã nghiệm thu cho tài khoản/sách/thành viên không bị ảnh hưởng; `.\mvnw.cmd --no-transfer-progress clean verify` thành công (25 test, 0 lỗi).
- [x] Migration Flyway V4 và mượn/trả được xác minh với volume PostgreSQL Compose hiện tại mà không xóa/reset; app và database khỏe mạnh.
- [x] Hai checklist song ngữ và danh sách file dự kiến khớp phạm vi đã triển khai/kiểm chứng.

#### Danh sách file dự kiến (cập nhật cả hai checklist trước khi thêm file ngoài kế hoạch)

- [x] `src/main/java/com/booknest/loan/Loan.java` — bản ghi mượn/trả, ngày hạn/ngày trả và nhân viên lập/nhận trả.
- [x] `src/main/java/com/booknest/loan/LoanRepository.java` — truy vấn lượt mượn, kiểm tra lượt đang hoạt động và hỗ trợ mượn/trả an toàn.
- [x] `src/main/java/com/booknest/loan/CheckoutRequest.java` và `LoanResponse.java` — input mượn đã validate và response an toàn có trạng thái đang mượn/quá hạn suy ra.
- [x] `src/main/java/com/booknest/loan/LoanService.java` — transaction mượn/trả, tính hạn 14 ngày, kiểm tra tham chiếu và luật nghiệp vụ.
- [x] `src/main/java/com/booknest/loan/LoanController.java` — endpoint mượn/trả yêu cầu đăng nhập.
- [x] `src/main/java/com/booknest/book/BookCopy.java`, `BookCopyRepository.java` và `BookCopyService.java` — thêm `ON_LOAN`, khóa bản sách được chọn và chặn sửa trạng thái/xóa khi đang mượn.
- [x] `src/main/resources/db/migration/V4__create_loans_and_loaned_copy_status.sql` — schema lượt mượn, khóa ngoại chặn xóa/ràng buộc/index và trạng thái `ON_LOAN`.
- [x] `src/test/java/com/booknest/loan/LoanControllerTests.java` — mượn/trả, ngày hạn/quá hạn, access, tham chiếu member/copy, giữ lịch sử và mượn đồng thời.
- [x] `src/test/java/com/booknest/book/BookCopyControllerTests.java` — không thể tự đặt `ON_LOAN`; loan tests xác minh không sửa/xóa được bản có lịch sử mượn.
- [x] `src/test/java/com/booknest/account/AccountControllerTests.java`, `src/test/java/com/booknest/security/SecurityConfigurationTests.java` và `src/test/java/com/booknest/member/MemberControllerTests.java` — dọn fixture lượt mượn trước khi xóa nhân viên/thành viên có liên kết để full suite tuân thủ khóa ngoại.
- [x] `src/main/resources/static/js/books.js` — hiển thị bản `ON_LOAN` dạng chỉ đọc trong UI tồn kho hiện tại.
- [x] `src/main/resources/static/js/api.js` — thông báo tiếng Việt an toàn cho conflict thành viên/lượt mượn.
- [x] `src/main/java/com/booknest/web/ApiExceptionHandler.java` — xử lý mã lỗi conflict nghiệp vụ an toàn cho mượn và bản ghi được tham chiếu.
- [x] `README.md` — tài liệu API mượn/trả, hạn 14 ngày, chuyển trạng thái và giới hạn hiện tại.
- [x] `AGENTS.md` — ghi quyết định Phase 4 đã duyệt cùng kết quả triển khai/kiểm chứng chính xác.
- [x] `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — duy trì tiêu chí và danh sách file đồng bộ.

**Trạng thái:** Backend Phase 4 đã triển khai và kiểm chứng, gồm migration PostgreSQL và test mượn đồng thời trực tiếp; chờ người dùng nghiệm thu. Chưa bắt đầu UI mượn hoặc phase sau.

### Lát cắt UI bổ sung — Mượn và trả (đã duyệt; đã kiểm chứng triển khai, chờ người dùng nghiệm thu)

Lát cắt UI này bổ sung luồng trình duyệt cho API Phase 4 đã được kiểm chứng. Người dùng đã duyệt phạm vi trước khi triển khai.

#### Phạm vi đã duyệt

- Thêm tab workspace accessible **Kho sách / Thành viên / Mượn-Trả**, giữ nguyên hành vi hiện tại của sách/bản sách/thành viên.
- Liệt kê lượt mượn với thành viên, đầu sách/bản sách, ngày mượn/hạn trả, trạng thái đang mượn/quá hạn/đã trả và nhân viên thao tác.
- Cho mượn bằng thành viên có sẵn và chỉ các bản hiện `AVAILABLE`; server vẫn là nguồn quyết định cuối cùng về độ sẵn sàng và hạn trả 14 ngày.
- Cho trả các lượt đang mượn sau khi xác nhận; tải lại danh sách mượn và tồn kho sau thao tác. Giữ lượt đã trả trong danh sách.
- Dùng `apiRequest` cùng origin hiện có cho session/CSRF; tạo DOM an toàn; có loading, empty, success, conflict/error, điều hướng bàn phím và giao diện responsive.
- Không seed demo, đặt trước, gia hạn, dashboard, tìm kiếm, lọc hoặc phân trang. Nếu database trống khi tự kiểm thử, tạo thành viên/đầu sách/bản sách `AVAILABLE` trước bằng UI hiện có.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã xem và duyệt phạm vi UI trước khi triển khai.
- [x] Nhân viên chuyển được giữa Kho sách, Thành viên và Mượn/Trả; màn hình cũ và điều hướng tab bằng bàn phím vẫn hoạt động.
- [x] Nhân viên xem được lượt đang mượn, quá hạn, đã trả cùng ngày và nhân viên thao tác; lượt đã trả vẫn hiển thị.
- [x] Dialog mượn liệt kê thành viên hiện có và chỉ các bản `AVAILABLE`; giải thích khi thiếu thành viên/bản sẵn sàng và khóa submit khi chưa đủ dữ liệu.
- [x] Thao tác mượn chỉ gửi ID thành viên/bản sách; sau đó hiển thị hạn 14 ngày do server tính và trạng thái tồn kho `ON_LOAN`.
- [x] Nhân viên xác nhận trả được sách; trạng thái lượt mượn và tồn kho `AVAILABLE` được cập nhật; conflict do bản vừa bị mượn được hiển thị an toàn.
- [x] Trạng thái tải, danh sách rỗng, lỗi API/mạng, conflict, thành công và xác nhận thao tác dễ hiểu/accessibility.
- [x] Nội dung loan/member/book được chèn an toàn qua DOM API; không HTML injection hoặc lưu dữ liệu nhạy cảm trong browser storage.
- [x] UI hoạt động ở desktop và viewport 360px không làm tràn ngang toàn tài liệu; bảng mượn cuộn trong vùng chứa.
- [x] Kiểm tra cú pháp toàn bộ JavaScript, `.\mvnw.cmd --no-transfer-progress clean verify` (25 test), Compose config/rebuild/readiness và luồng trình duyệt mượn/trả đều đạt; account/member/book/copy/loan tạm đã xóa và xác nhận không còn.
- [x] README, hướng dẫn agent và hai checklist khớp hành vi triển khai/kết quả kiểm chứng thực tế.

#### Danh sách file dự kiến

- [x] `src/main/resources/static/index.html` — thêm panel workspace mượn và dialog lập phiếu.
- [x] `src/main/resources/static/js/app.js` — khởi tạo module mượn và nối tab thứ ba/vòng đời xác thực.
- [x] `src/main/resources/static/js/loans.js` — tải/render lượt mượn, nạp lựa chọn, gửi yêu cầu mượn/trả qua API helper.
- [x] `src/main/resources/static/css/styles.css` — style trạng thái mượn và danh sách responsive/cuộn được.
- [x] `README.md` — mô tả UI mượn/trả và cách chuẩn bị dữ liệu để tự kiểm thử trên database trống.
- [x] `AGENTS.md` — ghi phạm vi UI đã duyệt và kết quả triển khai đã xác minh.
- [x] `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — đồng bộ tiêu chí nghiệm thu và danh sách file.

**Trạng thái:** Lát cắt UI đã duyệt, triển khai và kiểm chứng; chờ người dùng nghiệm thu trước khi làm phase tiếp.

### Lát cắt UI tài khoản — Đổi mật khẩu (đã duyệt; đã triển khai và kiểm chứng, chờ nghiệm thu)

Người dùng yêu cầu có chức năng đổi mật khẩu sau khi nhận thấy giao diện chưa có mục này. Người dùng đã chọn hành vi sau khi đổi: kết thúc phiên hiện tại và yêu cầu đăng nhập lại. Người dùng đã duyệt phạm vi, gồm hành vi đăng xuất phiên hiện tại sau khi đổi.

#### Phạm vi đề xuất

- Thêm thao tác **Đổi mật khẩu** dễ thấy trong khu vực tài khoản đã đăng nhập; không làm thay đổi đăng ký, đăng nhập hoặc đăng xuất hiện tại.
- Thêm API yêu cầu đăng nhập và chỉ đổi mật khẩu tài khoản nhân viên hiện tại; lấy account từ principal đã xác thực, tuyệt đối không nhận account ID từ client.
- Form yêu cầu mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu mới. Phải xác minh mật khẩu hiện tại; mật khẩu mới tuân thủ chính sách 8–72 ký tự hiện tại và không được trùng mật khẩu hiện tại.
- Chỉ lưu BCrypt hash mới. Không trả hoặc ghi log mật khẩu/hash. Giữ CSRF và session cùng origin hiện có.
- Khi thành công, hủy session hiện tại và yêu cầu đăng nhập lại bằng mật khẩu mới. Lát cắt này không chủ động thu hồi/theo dõi các session đang hoạt động khác; ghi rõ giới hạn này trừ khi phạm vi sau thay đổi.
- Hiển thị thông báo an toàn, dễ hiểu khi sai mật khẩu hiện tại, validation/xác nhận không khớp, lỗi mạng và đổi thành công/đăng xuất; không tiết lộ account hay dữ liệu mật khẩu.
- Không gồm reset mật khẩu qua email, recovery code, admin reset, thu hồi nhiều session hoặc quản lý tài khoản.

#### Tiêu chí nghiệm thu

- [x] Người dùng xem và duyệt phạm vi đổi mật khẩu cùng hành vi đăng xuất phiên hiện tại trước khi triển khai.
- [x] Nhân viên đã đăng nhập truy cập được form đổi mật khẩu; anonymous không gọi được API.
- [x] Mật khẩu hiện tại đúng cùng mật khẩu mới hợp lệ sẽ chỉ đổi BCrypt hash tài khoản đang đăng nhập; xác nhận mật khẩu phải khớp trên UI.
- [x] Sai mật khẩu hiện tại bị từ chối và không thay đổi mật khẩu đã lưu; thông báo an toàn, không lộ bí mật.
- [x] Mật khẩu mới ngắn hơn 8 hoặc dài hơn 72 ký tự, xác nhận không khớp và dùng lại mật khẩu hiện tại đều bị từ chối với giải thích rõ.
- [x] Sau khi đổi thành công, session hiện tại bị hủy và người dùng phải đăng nhập lại; mật khẩu mới đăng nhập được, mật khẩu cũ không đăng nhập được.
- [x] Request ghi dữ liệu yêu cầu CSRF; request lỗi nhận response an toàn/nhất quán. Client không gửi account ID.
- [x] Mật khẩu/hash không xuất hiện trong API response, log hoặc browser storage.
- [x] Đăng ký/đăng nhập/đăng xuất và các luồng Kho sách/Thành viên/Mượn-Trả vẫn hoạt động; `.\mvnw.cmd --no-transfer-progress clean verify` đạt (29 test, 0 lỗi/thất bại/bỏ qua), cú pháp toàn bộ JavaScript đạt, Compose được build lại không reset dữ liệu, readiness UP và browser checks đổi mật khẩu đạt.
- [x] Hai checklist song ngữ và hướng dẫn dự án ghi đúng triển khai/kết quả kiểm chứng thực tế.

#### Danh sách file dự kiến (cập nhật cả hai checklist trước khi thêm file ngoài dự kiến)

- [x] `src/main/java/com/booknest/account/AccountController.java` — thêm endpoint đổi mật khẩu đã xác thực và hủy phiên hiện tại khi thành công.
- [x] `src/main/java/com/booknest/account/AccountService.java` và `ChangePasswordRequest.java` — xác minh mật khẩu hiện tại, policy, mã hóa và cập nhật tài khoản hiện tại an toàn.
- [x] `src/main/java/com/booknest/account/StaffAccount.java` — thêm method cập nhật password hash có phạm vi hẹp.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java` — xác nhận rule `/api/**` yêu cầu đăng nhập cùng CSRF/session sẵn có bao phủ endpoint; không cần đổi cấu hình.
- [x] `src/test/java/com/booknest/account/AccountControllerTests.java` và `src/test/java/com/booknest/security/SecurityConfigurationTests.java` — kiểm thử thành công, sai/dùng lại mật khẩu, policy, authentication, CSRF, hủy session, đăng nhập mật khẩu mới và từ chối mật khẩu cũ; không lộ mật khẩu/hash.
- [x] `src/main/resources/static/index.html`, `src/main/resources/static/js/auth.js` và `src/main/resources/static/css/styles.css` — thêm thao tác/form accessible cùng trạng thái thành công/lỗi.
- [x] `src/main/resources/static/js/api.js` — dịch lỗi an toàn về đổi mật khẩu.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — ghi phạm vi, hành vi session, giới hạn và trạng thái kiểm chứng.

**Trạng thái:** UI/API đã triển khai và kiểm chứng theo phạm vi được duyệt; chờ người dùng nghiệm thu. Các session khác ngoài session hiện tại không bị thu hồi/theo dõi.

### Thay đổi mô hình tài khoản — Bạn đọc tự đăng ký và khôi phục mật khẩu (đã duyệt; đang triển khai)

Người dùng đã duyệt checklist chi tiết và việc triển khai. Hướng sản phẩm là một thư viện công khai dùng chung; bạn đọc tự tạo tài khoản và dùng chức năng mượn/đặt; nhân viên quản lý sách và hỗ trợ nghiệp vụ. Quy trình không dùng email yêu cầu admin xác minh người dùng rồi cấp mật khẩu tạm; admin tự khôi phục bằng thao tác vận hành trên máy/server.

#### Phạm vi đề xuất

- Có ba vai trò tách biệt và được kiểm tra ở backend: `ADMIN`, `STAFF`, `PATRON`. Không tin role, user ID, member ID hay actor ID do client gửi; không dựa riêng vào việc ẩn nút trên UI.
- Ma trận quyền đề xuất: visitor chỉ xem catalog công khai; patron xem/sửa hồ sơ của mình, mượn/đặt và xem giao dịch của mình; staff quản lý sách/bản sách, hồ sơ thành viên và nghiệp vụ thư viện; admin có toàn quyền, tạo/quản lý nhân viên và reset tài khoản. Chỉ admin được reset mật khẩu nhân viên/bạn đọc.
- Bạn đọc tự đăng ký tài khoản với username/mật khẩu. Khi đăng ký thành công, hệ thống tạo hoặc liên kết hồ sơ thành viên mượn tương ứng để không yêu cầu nhân viên nhập lại từng người. Dữ liệu tài khoản xác thực và hồ sơ thành viên vẫn là khái niệm riêng, có liên kết rõ ràng.
- Tái sử dụng quy tắc hồ sơ Member đã có: họ tên bắt buộc; email, điện thoại, ghi chú tùy chọn; chuẩn hóa dữ liệu tùy chọn trống thành null. Username duy nhất; đăng ký trùng username nhận lỗi an toàn.
- Catalog công khai cho phép người chưa đăng nhập xem đầu sách và số bản sẵn sàng, nhưng không xem dữ liệu cá nhân, tài khoản, lượt mượn hay thông tin bạn đọc khác.
- Bạn đọc đã đăng nhập có thể mượn ngay một bản `AVAILABLE`; server tính hạn theo chính sách hiện tại 14 ngày và vẫn chống tranh chấp mượn bản cuối. Nhân viên xác nhận trả sách sau khi nhận lại bản vật lý; việc trả không xóa lịch sử.
- Khi không có bản sẵn sàng, bạn đọc có thể tham gia hàng chờ FIFO cho đầu sách và hủy yêu cầu của chính mình. Khi trả sách, bản được giữ cho người đầu hàng đủ điều kiện theo chính sách hiện có; không dùng email hoặc scheduler hết hạn tự động. Bạn đọc chỉ xem lượt mượn/đặt của chính mình; nhân viên/admin xem và xử lý nghiệp vụ theo quyền.
- Trang đăng nhập có link **Quên mật khẩu?**. Vì không có email, link chỉ hướng dẫn người dùng liên hệ thư viện để admin xác minh và đặt lại; nhân viên có thể chuyển yêu cầu nhưng không tự reset. Tuyệt đối không có mật khẩu/cụm từ chung hoặc endpoint công khai cho phép đặt lại tài khoản bất kỳ.
- Admin có thể đặt lại mật khẩu riêng cho một bạn đọc hoặc nhân viên sau khi xác minh. Hệ thống sinh mật khẩu tạm ngẫu nhiên, chỉ hiển thị một lần, chỉ lưu hash, ghi nhận sự kiện an toàn và buộc tài khoản đó đổi mật khẩu ở lần đăng nhập kế tiếp. Mật khẩu tạm không phải mật khẩu dùng chung.
- Chỉ một admin cao nhất được bootstrap theo quy trình vận hành local/server có xác thực; username `admin` có thể được dành riêng nếu khả thi. Không hardcode `admin/admin`, không đưa mật khẩu admin vào repository, image hoặc trang web. Bootstrap/khôi phục admin tạo thông tin tạm ngẫu nhiên và bắt đổi sau đăng nhập.
- Nhân viên được tạo/cấp tài khoản bởi admin; nhân viên không phải tạo tài khoản cho từng bạn đọc. Admin có quyền quản lý tài khoản/nhân viên và khôi phục tài khoản; nhân viên quản lý đầu sách/bản sách và thực hiện các nghiệp vụ được chốt.
- Giữ session cookie, CSRF, BCrypt, policy mật khẩu hiện tại, session invalidation khi đổi mật khẩu và nguyên tắc không lộ secret. Admin/staff/patron chỉ truy cập API theo quyền tối thiểu cần thiết.
- Cập nhật/migrate schema theo cách không phá hủy dữ liệu hiện có; không xóa volume hoặc reset dữ liệu. Xác định cách phân loại các tài khoản cũ và giữ liên kết/lịch sử Member/Loan trước khi migration.
- Tài khoản nhân viên cũ được giữ lại và phân loại thành `STAFF`; hồ sơ Member cũ tiếp tục hoạt động, không tự ghép nhầm với tài khoản mới. Chỉ bổ sung liên kết cho đăng ký Patron mới. Tất cả migration là forward-only/an toàn; không hướng dẫn reset volume.
- Ngoài phạm vi nếu chưa được duyệt riêng: email/SMTP, OAuth, JWT, captcha, dịch vụ ngoài, khôi phục admin qua web công khai, role tùy biến và thu hồi mọi session.

#### Tiêu chí nghiệm thu (chỉ đánh dấu sau khi triển khai và kiểm chứng)

- [ ] Bạn đọc tự đăng ký tài khoản và tự mượn/đặt trong thư viện dùng chung; admin cao nhất; nhân viên quản lý thư viện; không khôi phục qua email.
- [ ] Đăng ký username trùng bị từ chối an toàn; tự tạo Member gắn với tài khoản bằng họ tên bắt buộc và email/điện thoại/ghi chú tùy chọn theo validation hiện có.
- [ ] Mượn có hiệu lực ngay nếu còn bản `AVAILABLE`; server tính hạn 14 ngày. Nhân viên xác nhận trả sách vật lý. Bạn đọc chỉ xem dữ liệu mượn/đặt của chính mình.
- [ ] Khi không còn bản sẵn sàng, bạn đọc có thể vào/hủy hàng chờ FIFO; giữ sách cho người đầu hàng đủ điều kiện khi trả; không có email hoặc tự động hết hạn.
- [ ] Người dùng duyệt ma trận quyền đề xuất: visitor chỉ catalog; patron chỉ dữ liệu của mình và tự mượn/đặt; staff quản lý sách/bản/member và giao dịch; admin quản lý mọi tài khoản, reset mật khẩu và toàn bộ nghiệp vụ. Backend từ chối mọi truy cập trái quyền.
- [ ] Link **Quên mật khẩu?** hướng dẫn liên hệ thư viện để admin xác minh/reset; nhân viên không tự reset; link không làm lộ tài khoản và không cho tự đặt lại khi chưa xác minh.
- [ ] Quy trình admin reset xác minh tài khoản đích, phát mật khẩu tạm ngẫu nhiên một lần, bắt đổi trước khi truy cập nghiệp vụ, không lưu/ghi log plaintext và audit actor/target/thời điểm an toàn.
- [ ] Mật khẩu tạm của bạn đọc/nhân viên chỉ dùng một lần để đăng nhập và đổi mật khẩu; các lần đăng nhập bình thường dùng mật khẩu họ tự đặt.
- [ ] Admin được bootstrap/khôi phục qua thao tác local/server được xác thực, không cần email, không có mật khẩu cố định/default trong source/config đã commit/UI; bắt đổi mật khẩu tạm.
- [ ] Đổi mật khẩu hiện tại tiếp tục hoạt động theo lựa chọn đã duyệt: kết thúc session hiện tại và yêu cầu đăng nhập lại.
- [ ] Thay đổi DB không làm mất account/member/book/copy/loan hiện có; migration và downgrade/khôi phục được mô tả rõ, không reset volume.
- [ ] Automated tests bao phủ đăng ký bạn đọc, liên kết member, phân quyền và cấm truy cập chéo, reset/first-login password, CSRF/session, không lộ secrets, bảo toàn dữ liệu; kiểm tra UI trên desktop/mobile.
- [ ] README, hướng dẫn agent và checklist song ngữ ghi chính xác cách bootstrap/khôi phục admin và giới hạn bảo mật; báo đúng lệnh/test đã chạy.

#### Checklist file triển khai

- [x] `src/main/java/com/booknest/account/` — role, đăng ký bạn đọc/nhân viên, tài khoản tạm, đổi/reset mật khẩu và bootstrap admin.
- [x] `src/main/java/com/booknest/member/` — liên kết tài khoản bạn đọc với hồ sơ thành viên mà không làm mất lịch sử.
- [x] `src/main/java/com/booknest/security/` — phân quyền backend, phiên bản credential, đường dẫn công khai và CSRF.
- [x] `src/main/resources/db/migration/` — migration tiến về phía trước cho role, trạng thái đổi mật khẩu, liên kết account-member, audit và phiên bản credential.
- [x] `src/main/resources/static/index.html`, `src/main/resources/static/js/` và `src/main/resources/static/css/styles.css` — đăng ký bạn đọc, hướng dẫn quên mật khẩu, hồ sơ và luồng theo role.
- [x] `src/test/java/com/booknest/` — kiểm thử role, khôi phục, quyền sở hữu hồ sơ, hủy session và nghiệp vụ.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md`, `PHASE_CHECKLIST.vi.md` — hướng dẫn triển khai và trạng thái.

**Trạng thái:** Checklist đã được duyệt; triển khai và bộ regression 40 test đã hoàn tất. Migration PostgreSQL trên Compose đã nâng V4→V6, giữ nguyên số lượng dữ liệu ban đầu (1 account, 1 book, 0 copy/member/loan); kiểm tra trình duyệt thật gồm catalog công khai, đăng ký/đăng nhập patron, mở/lưu hồ sơ, đặt/hủy chỗ và layout 360px. CLI khôi phục admin thực tế đã được kiểm chứng trên database PostgreSQL tạm biệt lập, gồm mật khẩu tạm bắt buộc đổi và audit; chỉ database tạm này được xóa sau thử nghiệm. Phase vẫn chờ người dùng nghiệm thu; chưa commit/push trước khi nghiệm thu.

### Lát cắt dữ liệu demo — thư viện mẫu local (đã duyệt; đã triển khai/kiểm chứng, chờ nghiệm thu)

Người dùng muốn có database local với dữ liệu để tự khám phá ứng dụng và đã duyệt bộ demo đầy đủ gồm tài khoản bạn đọc/nhân viên, không tạo admin mặc định. Đây là một lát cắt giới hạn của Phase 9, không phải cho phép triển khai toàn bộ Phase 9.

#### Phạm vi

- Thêm trình khởi tạo dữ liệu demo local có bật rõ ràng và an toàn khi chạy lặp: tài khoản nhân viên/bạn đọc mẫu, hồ sơ Member liên kết và bạn đọc vãng lai, đầu sách/bản sách ở nhiều trạng thái, lịch sử mượn và các trạng thái đặt chỗ.
- Không bao giờ tạo hoặc reset tài khoản admin. Mặc định tắt khởi tạo demo; ghi rõ credential chỉ dùng local/demo; giữ nguyên mọi dữ liệu hiện có.
- Chỉ tạo từng bản ghi demo một lần; không ghi đè mật khẩu, hồ sơ, thông tin sách, lịch sử mượn hoặc trạng thái do người dùng thay đổi khi khởi động lại. Báo lỗi rõ nếu username/ISBN demo đã bị dùng cho dữ liệu khác.
- Giữ quan hệ nghiệp vụ hợp lệ: giao dịch bạn đọc gắn với đúng hồ sơ; lượt mượn đang hoạt động/trạng thái bản sách và chỗ giữ bản sách phải nhất quán; nhân viên thao tác được tham chiếu đúng.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã duyệt phạm vi dữ liệu demo trước khi triển khai.
- [x] Demo mặc định tắt và chỉ khởi tạo khi bật tùy chọn demo local một cách tường minh.
- [x] Bộ demo có đăng nhập STAFF/PATRON, hồ sơ bạn đọc liên kết và một hồ sơ vãng lai, nhiều đầu sách/bản sách, ví dụ mượn đang hoạt động/quá hạn/đã trả và đặt chỗ đang giữ/đang chờ; không seed admin.
- [x] Mật khẩu demo chỉ lưu dưới dạng BCrypt hash và được ghi rõ chỉ dùng local/demo; xung đột không âm thầm thay tài khoản hay dữ liệu thư viện hiện có.
- [x] Khởi tạo nhiều lần không nhân bản account/member/book/copy/loan/reservation và không làm mất bản ghi có sẵn.
- [x] Kiểm thử xác minh nội dung seed, liên kết/quy tắc trạng thái, hành vi bật/tắt và chạy lặp an toàn.
- [x] Dữ liệu cũ trong PostgreSQL Compose của người dùng còn nguyên; kiểm chứng số lượng dữ liệu và đăng nhập/hiển thị mẫu thật mà không xóa/reset volume.
- [x] README, AGENTS.md và checklist song ngữ ghi đúng cách bật, tài khoản/mật khẩu demo và kết quả kiểm chứng thực tế.

#### Danh sách file dự kiến

- [x] `src/main/java/com/booknest/demo/DemoDataInitializer.java` — thêm bộ dữ liệu mẫu có bật tường minh, transactional và chạy lặp an toàn.
- [x] `src/main/java/com/booknest/demo/DemoSeedRun.java` và `DemoSeedRunRepository.java` — ghi nhận seed hoàn tất để lần khởi động sau giữ nguyên dữ liệu demo người dùng đã sửa.
- [x] `src/main/resources/db/migration/V7__track_demo_seed_run.sql` — thêm bảng đánh dấu tiến về phía trước; không sửa/xóa dữ liệu thư viện cũ.
- [x] `src/main/resources/application.yml`, `compose.yaml` và `.env.example` — nối cờ demo với giá trị mặc định tắt.
- [x] `src/test/java/com/booknest/demo/DemoDataInitializerTests.java` — kiểm thử bật/tắt, liên kết dữ liệu và tính idempotent.
- [x] `src/test/java/com/booknest/BookNestApplicationTests.java` — xác minh không có initializer khi chế độ demo tắt.
- [x] `README.md` — hướng dẫn bật demo local, tài khoản/mật khẩu, bản ghi mẫu và nguyên tắc không tạo admin.
- [x] `AGENTS.md`, `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — ghi phạm vi đã duyệt, ledger và kết quả kiểm chứng thực tế.

**Trạng thái:** Lát cắt dữ liệu demo đã duyệt, triển khai, kiểm chứng và được người dùng nghiệm thu. `.\mvnw.cmd --no-transfer-progress clean verify` đạt 45 test, 0 lỗi/thất bại/bỏ qua; `docker compose config --quiet` đạt và Docker image cuối đã build thành công. Dữ liệu PostgreSQL cũ được giữ nguyên khi migrate Flyway V6→V7. Số lượng trước/sau seed chuyển từ 3 account/1 book/0 copy/1 member/0 loan/0 reservation sang 6/4/9/4/3/4, cộng một seed marker. Khởi động lại app giữ nguyên số lượng, readiness HTTP 200 và Compose hiện chạy với khởi tạo demo đã tắt. Kiểm tra trình duyệt xác nhận catalog công khai, tài khoản staff xem sách/member/loan/hàng chờ và patron chỉ xem loan cùng đặt chỗ của chính mình. Không volume/database/container hay tài nguyên Docker ngoài phạm vi nào bị xóa/reset.

### Phase 5 — Bổ sung gia hạn lượt mượn (hàng chờ đã có; đã triển khai, chờ nghiệm thu)

Phần đặt trước đã triển khai trước đó gồm hàng chờ FIFO, giữ sách khi trả và hủy/đẩy hàng chờ. Lát cắt này bổ sung chính sách gia hạn một lần và nút thao tác trên giao diện; không viết lại hoặc làm hỏng hàng chờ hiện có.

#### Phạm vi đã triển khai

- Mỗi lượt mượn được gia hạn tối đa một lần. Chỉ gia hạn lượt đang mượn và chưa quá hạn; từ chối nếu đầu sách có lượt đặt trước đang `WAITING` hoặc `HELD`.
- Patron chỉ gia hạn lượt của chính mình; staff/admin có thể gia hạn để hỗ trợ nghiệp vụ thư viện. Lấy tài khoản thao tác từ session đã xác thực, tuyệt đối không nhận account/member/staff ID từ client.
- Lưu trạng thái đã gia hạn, thời điểm và tài khoản thực hiện để bản ghi nghiệp vụ giữ được người thao tác.
- Thêm endpoint gia hạn có transaction. Khóa lượt mượn và đầu sách theo cùng thứ tự với luồng trả hiện tại; đồng bộ gia hạn với tạo/hủy đặt chỗ để bạn đọc mới không bị bỏ qua do request đồng thời.
- Bổ sung trạng thái gia hạn trong API và hiển thị nút **Gia hạn** cùng kết quả trên giao diện Mượn/Hoạt động hiện tại. Backend luôn là nguồn quyết định; UI ẩn/vô hiệu hóa thao tác khi biết chắc không hợp lệ và hiển thị lỗi conflict an toàn, có tiếng Việt.
- Giữ nguyên mượn, trả, quyền sở hữu patron, hàng chờ, CSRF và session. Không tự động gia hạn, email, gia hạn thêm lần nữa, gia hạn lượt quá hạn hoặc làm hệ thống event-sourcing/lịch sử phức tạp.

#### Cần người dùng chọn chính sách trước khi triển khai

Người dùng chọn kỳ hạn gia hạn 14 ngày lịch hiện tại, tính từ hạn trả đang có:

- **Đã duyệt:** cộng thêm 14 ngày lịch vào hạn trả hiện tại.

#### Tiêu chí nghiệm thu

- [x] Người dùng đã chọn quy tắc tính ngày hạn trước khi triển khai: cộng 14 ngày lịch vào hạn trả hiện tại.
- [x] Patron hợp lệ gia hạn được lượt của mình khi đang mượn, chưa quá hạn và chưa gia hạn; chỉ thành công đúng một lần. Staff/admin có thể gia hạn cho nghiệp vụ; patron không thể gia hạn lượt của người khác.
- [x] Lượt đã trả, quá hạn, đã gia hạn hoặc bị đặt trước chặn đều bị từ chối bằng conflict an toàn; trạng thái loan/copy/hàng chờ không đổi.
- [x] Hạn mới đúng chính sách 14 ngày lịch người dùng chọn. API/UI hiển thị trạng thái và actor gia hạn, không nhận actor ID từ client.
- [x] Nhiều yêu cầu gia hạn đồng thời chỉ có tối đa một request thành công; tạo hàng chờ đồng thời với gia hạn không bỏ qua bạn đọc hoặc để lại trạng thái dang dở (test đồng thời tự động dùng H2).
- [x] UI chỉ hiển thị thao tác phù hợp, tải lại lượt mượn/hoạt động sau thành công, có trạng thái tải/thành công/lỗi mạng/conflict accessible và an toàn.
- [x] Mượn, trả, đặt chỗ, authentication/authorization, CSRF và responsive hiện tại không bị ảnh hưởng bởi lát cắt này. Browser ở 360px phát hiện account bar khi đăng nhập (lỗi đã có từ trước), làm tràn toàn trang; bảng loan vẫn nằm trong vùng cuộn ngang. Phần gia hạn không thay CSS header; lỗi account bar được theo dõi riêng.
- [x] Test tập trung bao phủ chính sách, role/quyền sở hữu, đồng thời/tính nhất quán và công thức ngày hạn đã chọn; ghi chính xác kết quả `clean verify`, Compose, migration/build với volume PostgreSQL hiện có và luồng trình duyệt.
- [x] README, AGENTS.md và hai checklist song ngữ mô tả chính sách gia hạn đã triển khai và kết quả kiểm chứng.

#### Danh sách file dự kiến

- [x] `src/main/java/com/booknest/loan/Loan.java`, `LoanService.java`, `LoanRepository.java`, `LoanController.java` và `LoanResponse.java` — trạng thái, chính sách, khóa transaction và API gia hạn.
- [x] `src/main/resources/db/migration/V8__add_loan_renewal.sql` — bổ sung theo hướng tiến lên, giữ nguyên lịch sử lượt mượn.
- [x] `src/main/java/com/booknest/reservation/ReservationRepository.java` và `ReservationService.java` — truy vấn reservation đang chờ/đang giữ để kiểm tra điều kiện gia hạn.
- [x] `src/main/java/com/booknest/web/ApiExceptionHandler.java` và `src/main/resources/static/js/api.js` — ánh xạ lỗi gia hạn an toàn.
- [x] `src/main/resources/static/js/loans.js` — nút gia hạn theo role và trạng thái, không cần sửa markup/CSS.
- [x] `src/test/java/com/booknest/loan/LoanControllerTests.java` — chính sách, quyền sở hữu, CSRF, gia hạn đồng thời và tạo hàng chờ đồng thời.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — tài liệu hóa chính sách, triển khai và kết quả xác minh.

**Kiểm chứng:** `.\mvnw.cmd --no-transfer-progress clean verify` đạt 51 test, 0 lỗi/thất bại/bỏ qua. Kiểm tra cú pháp JavaScript, `git diff --check` và `docker compose config --quiet` đều đạt. `docker compose up -d --build` build lại app và chạy với volume PostgreSQL hiện có; Flyway V8 áp dụng thành công, readiness HTTP 200 với app/DB UP. Truy vấn DB ghi nhận 6 account, 4 book, 9 copy, 4 member, 5 loan, 6 reservation và đúng 1 loan đã gia hạn. Loan demo-patron có hạn mới 2026-10-31 và actor `demo-patron`. Trình duyệt xác nhận hộp thoại, hạn đổi từ 2026-10-17 sang 2026-10-31, actor/trạng thái vẫn còn sau khi app restart và đăng nhập lại, nút gia hạn không còn hiện. Toàn trang khi đăng nhập còn tràn ngang ở 360px do account bar có từ trước; bảng loan vẫn cuộn ngang bên trong wrapper.

**Trạng thái:** Người dùng đã nghiệm thu ngày 2026-10-06. Lỗi account bar đã có từ trước khiến tràn ngang tại 360px được ghi nhận riêng, không thuộc lát cắt này.

### Phase 6 — Lịch sử hoạt động và danh sách tìm kiếm, phân trang

Phase này bổ sung timeline hoạt động chỉ-ghi-thêm và khả năng tìm kiếm dữ liệu theo trang trong database cho danh mục sách và lượt mượn. Backend phải thực thi quyền: staff/admin xem dữ liệu thư viện chung; patron chỉ xem lượt mượn và hoạt động của chính mình.

#### Phạm vi triển khai đã thống nhất

- Thêm cấu trúc response phân trang dùng chung: trang bắt đầu từ 0, mặc định 20 bản ghi/trang, tối đa 100 và có metadata ổn định. Từ chối page âm, size ngoài 1–100, sort field không cho phép và sort direction không hợp lệ bằng lỗi 400 an toàn.
- Sách: tìm kiếm không phân biệt hoa thường trên title, author, ISBN; lọc tùy chọn theo genre chính xác và tình trạng còn bản; chỉ cho sort theo title, author, genre, publication year hoặc ID. Search/filter/sort/paging phải chạy tại PostgreSQL, không lấy hết rồi lọc trong bộ nhớ.
- Lượt mượn: tìm kiếm không phân biệt hoa thường theo tên sách và thành viên; lọc tùy chọn `ACTIVE`, `OVERDUE`, `RETURNED`; sort allowlist theo ngày mượn, hạn trả, ngày trả, tên sách hoặc tên thành viên. Tính quá hạn theo ngày hiện tại. Mọi query phải giữ giới hạn sở hữu dữ liệu của patron.
- Ghi lại event có timestamp cho mượn, trả, gia hạn, tạo đặt trước/giữ sách/hủy/hoàn tất đặt trước, kèm username người thao tác và snapshot an toàn. Không lưu mật khẩu, credential, session/CSRF hay thông tin liên hệ không cần thiết. Activity snapshot không được cản xóa sách/thành viên/tài khoản vốn hợp lệ; không dùng cascade hoặc giả định quan hệ sở hữu FK.
- Timeline mới bắt đầu ghi các hành động từ sau migration V9. Không tự dựng actor/thời điểm chuyển trạng thái đặt trước cũ; lịch sử loan cũ vẫn xem được từ bản ghi loan cùng actor/ngày đã lưu.
- Tạo endpoint activity có xác thực, phân trang mới nhất trước. Staff/admin xem sự kiện thư viện chung; patron chỉ xem sự kiện gắn với account/member của mình, giới hạn ngay trong query backend.
- Thêm điều khiển tìm kiếm/lọc/sort/trang trên giao diện Books và Loans cùng một khu lịch sử có phân trang. Mã hóa query params, render bằng DOM an toàn, đổi filter thì về trang 0, giữ trạng thái loading/empty/error accessible.
- Giữ nguyên behavior/response mà màn chi tiết, checkout, đặt trước và role hiện tại phụ thuộc vào; cập nhật mọi nơi tiêu thụ list khi đổi response sang dạng phân trang. Không thêm dashboard, phân trang member, export hay audit payload không giới hạn.

#### Tiêu chí nghiệm thu

- [x] Tìm sách/lọc genre/availability, sort allowlist và phân trang DB trả đúng dữ liệu/tổng; sort ổn định khi trùng giá trị.
- [x] Tìm/lọc trạng thái loan, sort allowlist và phân trang chạy cho staff/admin, được giới hạn nghiêm ngặt cho patron. Overdue tính từ ngày hiện tại.
- [x] Page âm, size 0 hoặc trên 100, sort field lạ, direction/status/type sai đều bị từ chối an toàn; client không thể chèn SQL/property tùy ý.
- [x] Checkout, return, renewal và tạo/giữ/hủy/hoàn tất reservation ghi event chính xác, bất biến trong cùng transaction với nghiệp vụ, dùng actor lấy từ server và snapshot an toàn.
- [x] Staff/admin xem activity dùng chung; patron không xem được event hoặc dữ liệu riêng của patron khác; anonymous bị từ chối.
- [x] Migration V9 không mất dữ liệu cũ và giữ nguyên lịch sử loan; xóa catalog/member/account hợp lệ không bị activity snapshot cản trở.
- [x] Đã chạy UI search/filter với PostgreSQL ở các vai trò staff/patron, kết quả render bằng DOM an toàn. Tình trạng tràn ngang toàn trang ở 360px đã được ghi nhận từ trước do account bar khi đăng nhập; thay đổi lần này không sửa header đó.
- [x] Test tập trung bao phủ query, metadata/tổng trang, sort allowlist ổn định, input sai, privacy/authorization, tất cả loại event, rollback transaction và tương thích migration. Full verification, Compose, migration/readiness và browser test đều đạt, ghi cụ thể trong `AGENTS.md`.
- [x] README, AGENTS.md và checklist song ngữ ghi đúng API, history scope và kết quả kiểm chứng thực tế.

#### Danh sách file dự kiến

- [x] `src/main/java/com/booknest/common/PageResponse.java` và `PageRequestFactory.java` — metadata phân trang nhất quán, giới hạn input.
- [x] `src/main/java/com/booknest/activity/ActivityEvent.java`, `ActivityEventType.java`, `ActivityEventRepository.java`, `ActivityService.java`, `ActivityController.java` và `ActivityResponse.java` — snapshot bất biến, history có giới hạn quyền và endpoint.
- [x] `src/main/resources/db/migration/V9__create_activity_events.sql` — migration cộng thêm và index activity, giữ nguyên bản ghi cũ.
- [x] `src/main/java/com/booknest/book/BookRepository.java`, `BookCopyRepository.java`, `BookService.java`, `BookController.java`, `BookCopyService.java` và `BookCopyController.java` — filter/sort/page sách bằng database, truyền actor khi giữ reservation và tương thích list consumer hiện có.
- [x] `LoanRepository.java`, `LoanService.java` và `LoanController.java` — filter/sort/page loan trong khi giữ ownership và điều kiện gia hạn.
- [x] `src/main/java/com/booknest/reservation/ReservationRepository.java`, `ReservationService.java` và `loan/LoanService.java` — ghi event vòng đời với actor xác thực từ server, cùng transaction.
- [x] `src/main/java/com/booknest/book/BookCopyService.java` và `BookCopyController.java` — giữ actor đã xác thực khi thao tác chuyển copy sang sẵn sàng làm sách chờ được giữ.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java`, `web/ApiExceptionHandler.java` và `static/js/api.js` — bảo vệ activity, xử lý/hiển thị an toàn lỗi query parameter.
- [x] `src/main/resources/static/index.html`, `css/styles.css`, `js/books.js`, `js/loans.js`, `js/reservations.js` và `js/app.js` — điều khiển discovery accessible và activity timeline; giữ render DOM an toàn.
- [x] `src/test/java/com/booknest/book/BookControllerTests.java`, `loan/LoanControllerTests.java`, `reservation/ReservationControllerTests.java` và test mới `activity/ActivityControllerTests.java` — search/paging/sort, lifecycle, quyền, privacy và rollback.
- [x] `src/test/resources/cleanup.sql` — cô lập activity row giữa các test.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — tài liệu hóa API/UI và kết quả kiểm chứng.

**Trạng thái:** Người dùng đã nghiệm thu ngày 2026-10-06; đã xuất bản trong commit `a100990`. `.\mvnw.cmd --no-transfer-progress clean verify` đạt 59 test, 0 lỗi/failure/skip. JavaScript syntax, `git diff --check`, `docker compose config --quiet`, rebuild Compose, migration V9, readiness HTTP 200 của app/DB, query PostgreSQL cho catalog/search/loan/activity, và browser flow staff/patron đều đạt. Dữ liệu PostgreSQL hiện có được giữ nguyên. Tình trạng account bar gây tràn ngang ở viewport 360px đã được ghi nhận từ phase trước và nằm ngoài phạm vi Phase 6.

### Phase 7 — Dashboard thư viện dùng chung

Phase này bổ sung bảng tổng quan vận hành gọn dành cho nhân viên và admin. Theo lựa chọn của người dùng, patron không xem số liệu vận hành tổng hợp mà tiếp tục dùng màn lượt mượn/đặt trước/hoạt động cá nhân.

#### Phạm vi đã được duyệt

- Thêm endpoint tóm tắt dashboard yêu cầu đăng nhập, chỉ cho `STAFF` và `ADMIN`. Dùng dữ liệu đang có; không seed demo và không migration schema.
- Trả số lượng bản sách sẵn sàng, lượt mượn đang hoạt động, lượt mượn quá hạn đang hoạt động, và yêu cầu đặt trước còn hiệu lực. Loan đang hoạt động khi chưa có ngày trả; quá hạn là phần loan đang hoạt động có hạn trả trước hôm nay. Reservation còn hiệu lực gồm `WAITING` và `HELD`; do đó loan quá hạn là một phần của tổng loan đang mượn, không cộng thêm vào số loan đang hoạt động.
- Tính count bằng truy vấn tổng hợp/count ở database; không tải danh sách không giới hạn vào bộ nhớ ứng dụng. Thư viện trống trả 0 cho mọi số liệu.
- Hiển thị số liệu dùng chung trong khu tổng quan cho staff/admin; ẩn dashboard vận hành với patron và khách chưa đăng nhập. Tách biệt các con số danh mục đang hiển thị theo trang/lọc hiện tại.
- Làm mới dashboard sau thay đổi tồn kho, mượn, trả, gia hạn, đặt trước và giữ sách; đồng thời tải khi staff/admin đăng nhập hoặc quay lại dashboard. Có trạng thái loading/lỗi accessible và an toàn.
- Tài liệu hóa API và định nghĩa số liệu. Không thêm biểu đồ, khoảng thời gian, xuất file hay phân tích tổng hợp patron.

#### Tiêu chí nghiệm thu

- [x] Dashboard đếm đúng bản sách sẵn sàng, loan đang mượn/quá hạn và reservation đang chờ/được giữ; loại loan đã trả và reservation đã hủy/hoàn tất phù hợp.
- [x] Thư viện trống trả số 0; thay đổi nghiệp vụ cập nhật số liệu đang hiển thị mà không tải lại toàn trang.
- [x] Staff/admin lấy được cùng số liệu dùng chung; patron và anonymous không thể truy cập dashboard vận hành.
- [x] Count được tính trong database, không tải toàn bộ danh sách loan/copy/reservation.
- [x] Test bao phủ ranh giới trạng thái, quyền theo vai trò, số 0 và cập nhật chỉ số sau thay đổi nghiệp vụ.
- [x] README, AGENTS.md và checklist song ngữ ghi endpoint, định nghĩa chỉ số, kết quả kiểm chứng cụ thể và giới hạn nếu có.

#### Danh sách file dự kiến

- [x] `src/main/java/com/booknest/dashboard/DashboardController.java`, `DashboardService.java` và `DashboardSummary.java` — endpoint có phân quyền, tổng hợp count, cấu trúc response ổn định.
- [x] `src/main/java/com/booknest/book/BookCopyRepository.java`, `loan/LoanRepository.java` và `reservation/ReservationRepository.java` — truy vấn count database hiệu quả, xét đúng trạng thái.
- [x] `src/main/java/com/booknest/security/SecurityConfig.java` — chỉ staff/admin được gọi dashboard.
- [x] `src/main/resources/static/index.html`, `css/styles.css`, `js/dashboard.js` và `js/app.js` — tổng quan responsive, refresh/error theo vai trò.
- [x] `src/test/java/com/booknest/dashboard/DashboardControllerTests.java` — số liệu, ranh giới trạng thái, số 0 và authorization.
- [x] `README.md`, `AGENTS.md`, `PHASE_CHECKLIST.md` và `PHASE_CHECKLIST.vi.md` — hợp đồng API, phạm vi đã duyệt, ledger và kết quả kiểm chứng.

**Trạng thái:** Người dùng đã nghiệm thu ngày 2026-10-06 và Phase đã được xuất bản lên GitHub. `.\mvnw.cmd --no-transfer-progress clean verify` đạt 62 test (0 failure/error/skip); kiểm tra cú pháp JavaScript, `git diff --check` và `docker compose config --quiet` đều đạt. Compose đã build lại trên volume PostgreSQL hiện có; readiness của app và database trả HTTP 200/UP, V9 vẫn là migration mới nhất và không có thay đổi schema. Phiên trình duyệt STAFF hiển thị đủ bốn số liệu; API trả `availableCopies=2`, `activeLoans=3`, `overdueLoans=1`, `activeReservations=3`, khớp truy vấn count trực tiếp PostgreSQL `2|3|1|3`. API anonymous trả 401; phiên PATRON không có dashboard vận hành và API trả 403. Không reset dữ liệu hay volume.
