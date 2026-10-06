const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

const ERROR_MESSAGES = {
	invalid_credentials: "The username or password is incorrect.",
	invalid_current_password: "The current password is incorrect.",
	password_unchanged: "The new password must differ from the current password.",
	unauthorized: "Your session has expired. Please sign in again.",
	session_expired: "Your session has been revoked. Please sign in again.",
	password_change_required: "Change your temporary password before continuing.",
	forbidden: "The security request is invalid. Reload the page and try again.",
	validation_failed: "Please review the information you entered.",
	invalid_request: "The submitted data is invalid.",
	invalid_query_parameter: "The search or pagination parameters are invalid.",
	invalid_page: "The page number is invalid.",
	invalid_page_size: "Page size must be between 1 and 100.",
	invalid_sort_field: "This sort field is not supported.",
	invalid_sort_direction: "Sort direction must be ascending or descending.",
	invalid_availability_filter: "The book availability filter is invalid.",
	invalid_loan_state: "The loan status filter is invalid.",
	invalid_activity_type: "The activity type filter is invalid.",
	username_taken: "This username is already in use.",
	isbn_taken: "This ISBN is already in the library.",
	invalid_isbn_format: "ISBN must contain 10 or 13 digits; hyphens are optional.",
	invalid_publication_year: "The publication year is invalid.",
	book_not_found: "This book could not be found.",
	book_copy_not_found: "This copy could not be found.",
	book_has_copies: "Remove the book's copies before deleting the title.",
	book_copy_unavailable: "This copy is not currently available for checkout.",
	book_copy_reserved: "This copy is being held for a reservation.",
	book_copy_on_loan: "This copy is currently on loan.",
	book_copy_has_loan_history: "This copy cannot be deleted because it has loan history.",
	copy_status_managed_by_loans: "Copy availability changes through checkout and return actions.",
	member_has_loan_history: "This member cannot be deleted because they have loan history.",
	loan_not_found: "This loan could not be found.",
	loan_already_returned: "This loan has already been returned.",
	loan_not_active: "Only active loans can be renewed.",
	loan_overdue: "Overdue loans cannot be renewed.",
	loan_already_renewed: "This loan has already been renewed.",
	loan_has_reservation_queue: "A patron is waiting for or holding this title, so it cannot be renewed.",
	member_not_found: "This member could not be found.",
	account_not_found: "This account could not be found.",
	admin_recovery_requires_host_access: "Administrator accounts can only be recovered from the server.",
	member_required: "Please select a member.",
	member_account_linked: "A profile linked to a sign-in account cannot be deleted.",
	book_available_for_checkout: "A copy is available to borrow now.",
	reservation_already_active: "You already have an active reservation for this title.",
	reservation_not_active: "This reservation is no longer active.",
	reservation_not_ready: "No copy is currently being held for this reservation.",
	reservation_copy_unavailable: "The held copy is no longer available.",
	patron_profile_missing: "Your account has no borrowing profile. Please contact the library.",
	reservation_not_found: "This reservation could not be found.",
	reservation_book_missing: "The title for this reservation no longer exists.",
	reservation_copy_missing: "The copy for this reservation no longer exists.",
	data_conflict: "This change conflicts with existing data."
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
		return ERROR_MESSAGES[error.code] || error.message || "The request could not be completed.";
	}
	return "Could not connect to the server. Check the application and try again.";
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
				throw new ApiError(response.status, "csrf_unavailable", "Could not retrieve security information.");
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
			ERROR_MESSAGES[code] || payload.message || "The request could not be completed."
		);
	}
	return payload;
}
