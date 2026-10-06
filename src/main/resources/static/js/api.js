const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

const ERROR_MESSAGES = {
	invalid_credentials: "Tên đăng nhập hoặc mật khẩu chưa chính xác.",
	invalid_current_password: "Mật khẩu hiện tại chưa chính xác.",
	password_unchanged: "Mật khẩu mới phải khác mật khẩu hiện tại.",
	unauthorized: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
	session_expired: "Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.",
	password_change_required: "Hãy đổi mật khẩu tạm trước khi tiếp tục.",
	forbidden: "Yêu cầu bảo mật không hợp lệ. Hãy tải lại trang rồi thử lại.",
	validation_failed: "Vui lòng kiểm tra lại các trường thông tin.",
	invalid_request: "Dữ liệu gửi lên chưa hợp lệ.",
	invalid_query_parameter: "Tham số tìm kiếm hoặc phân trang chưa hợp lệ.",
	invalid_page: "Số trang không hợp lệ.",
	invalid_page_size: "Số mục mỗi trang phải từ 1 đến 100.",
	invalid_sort_field: "Trường sắp xếp không được hỗ trợ.",
	invalid_sort_direction: "Thứ tự sắp xếp chỉ nhận tăng dần hoặc giảm dần.",
	invalid_availability_filter: "Bộ lọc tình trạng sách không hợp lệ.",
	invalid_loan_state: "Bộ lọc trạng thái mượn không hợp lệ.",
	invalid_activity_type: "Bộ lọc hoạt động không hợp lệ.",
	username_taken: "Tên đăng nhập này đã được sử dụng.",
	isbn_taken: "ISBN này đã có trong thư viện.",
	invalid_isbn_format: "ISBN cần có 10 hoặc 13 chữ số (có thể nhập dấu gạch).",
	invalid_publication_year: "Năm xuất bản chưa hợp lệ.",
	book_not_found: "Không tìm thấy đầu sách này.",
	book_copy_not_found: "Không tìm thấy bản sách này.",
	book_has_copies: "Hãy xóa các bản sách trước khi xóa đầu sách.",
	book_copy_unavailable: "Bản sách hiện không sẵn sàng để mượn.",
	book_copy_reserved: "Bản sách đang được giữ cho lượt đặt trước.",
	book_copy_on_loan: "Bản sách đang được mượn.",
	book_copy_has_loan_history: "Không thể xóa bản sách vì đã có lịch sử mượn.",
	copy_status_managed_by_loans: "Trạng thái mượn được cập nhật qua thao tác mượn và trả sách.",
	member_has_loan_history: "Không thể xóa thành viên vì đã có lịch sử mượn.",
	loan_not_found: "Không tìm thấy lượt mượn này.",
	loan_already_returned: "Lượt mượn này đã được trả trước đó.",
	loan_not_active: "Chỉ lượt mượn đang hoạt động mới có thể gia hạn.",
	loan_overdue: "Không thể gia hạn lượt mượn đã quá hạn.",
	loan_already_renewed: "Lượt mượn này đã được gia hạn một lần.",
	loan_has_reservation_queue: "Có bạn đọc đang chờ hoặc đã được giữ sách; không thể gia hạn lúc này.",
	member_not_found: "Không tìm thấy thành viên này.",
	account_not_found: "Không tìm thấy tài khoản này.",
	admin_recovery_requires_host_access: "Tài khoản admin chỉ được khôi phục từ máy chủ.",
	member_required: "Vui lòng chọn thành viên.",
	member_account_linked: "Không thể xóa hồ sơ đang gắn với tài khoản đăng nhập.",
	book_available_for_checkout: "Đang có bản sẵn sàng để mượn ngay.",
	reservation_already_active: "Bạn đã có yêu cầu đặt trước còn hiệu lực cho đầu sách này.",
	reservation_not_active: "Yêu cầu đặt trước này không còn hiệu lực.",
	reservation_not_ready: "Chưa có bản sách được giữ cho yêu cầu này.",
	reservation_copy_unavailable: "Bản được giữ không còn sẵn sàng.",
	patron_profile_missing: "Tài khoản bạn đọc chưa có hồ sơ mượn. Hãy liên hệ thư viện.",
	reservation_not_found: "Không tìm thấy yêu cầu đặt trước này.",
	reservation_book_missing: "Đầu sách của yêu cầu đặt trước không còn tồn tại.",
	reservation_copy_missing: "Bản sách của yêu cầu đặt trước không còn tồn tại.",
	data_conflict: "Dữ liệu đang xung đột với thông tin đã có."
};

let csrfState = null;
let csrfRequest = null;

export class ApiError extends Error {
	constructor(status, code, message) {
		super(message);
		this.name = "ApiError";
		this.status = status;
		this.code = code;
	}
}

export function userMessage(error) {
	if (error instanceof ApiError) {
		return ERROR_MESSAGES[error.code] || error.message || "Không thể hoàn tất yêu cầu.";
	}
	return "Không kết nối được với máy chủ. Hãy kiểm tra ứng dụng và thử lại.";
}

export function clearCsrfToken() {
	csrfState = null;
}

async function getCsrfToken() {
	if (csrfState) {
		return csrfState;
	}
	if (!csrfRequest) {
		csrfRequest = fetch("/api/auth/csrf", {
			credentials: "same-origin",
			headers: { Accept: "application/json" },
			cache: "no-store"
		}).then(async response => {
			if (!response.ok) {
				throw new ApiError(response.status, "csrf_unavailable", "Không lấy được thông tin bảo mật.");
			}
			csrfState = await response.json();
			return csrfState;
		}).finally(() => {
			csrfRequest = null;
		});
	}
	return csrfRequest;
}

export async function apiRequest(path, options = {}) {
	const method = (options.method || "GET").toUpperCase();
	const headers = new Headers(options.headers || {});
	headers.set("Accept", "application/json");

	if (options.body !== undefined && !(options.body instanceof URLSearchParams) && !headers.has("Content-Type")) {
		headers.set("Content-Type", "application/json");
	}

	if (MUTATING_METHODS.has(method)) {
		const csrf = await getCsrfToken();
		headers.set(csrf.headerName, csrf.token);
	}

	const response = await fetch(path, {
		...options,
		method,
		headers,
		credentials: "same-origin",
		cache: "no-store"
	});

	if (response.status === 204) {
		return null;
	}

	const contentType = response.headers.get("content-type") || "";
	const payload = contentType.includes("application/json")
		? await response.json()
		: { message: await response.text() };

	if (!response.ok) {
		const code = payload.error || "request_failed";
		if (code === "session_expired" && path !== "/api/auth/me") {
			window.dispatchEvent(new Event("booknest-session-expired"));
		}
		throw new ApiError(
			response.status,
			code,
			ERROR_MESSAGES[code] || payload.message || "Không thể hoàn tất yêu cầu."
		);
	}
	return payload;
}
