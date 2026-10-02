package com.booknest.web;

import java.util.Map;

import com.booknest.account.StaffAccountAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(StaffAccountAlreadyExistsException.class)
	public ResponseEntity<Map<String, String>> handleDuplicateUsername(
			StaffAccountAlreadyExistsException exception
	) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("error", "username_taken", "message", "That username is already in use."));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleDataConflict(DataIntegrityViolationException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("error", "data_conflict", "message", "The request conflicts with existing data."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationFailure(MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest()
				.body(Map.of("error", "validation_failed", "message", "Request validation failed."));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadableRequest(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest()
				.body(Map.of("error", "invalid_request", "message", "Request body is invalid."));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleRequestFailure(ResponseStatusException exception) {
		HttpStatusCode status = exception.getStatusCode();
		String error = exception.getReason() == null ? "request_failed" : exception.getReason();
		String message = switch (status.value()) {
			case 400 -> "Request validation failed.";
			case 404 -> "The requested resource was not found.";
			case 409 -> switch (error) {
				case "member_has_loan_history" -> "The member cannot be deleted because loan history exists.";
				case "book_copy_has_loan_history" -> "The copy cannot be deleted because loan history exists.";
				case "book_copy_unavailable" -> "The copy is not available for checkout.";
				case "book_copy_on_loan" -> "The copy is currently on loan.";
				case "copy_status_managed_by_loans" ->
						"Loan status is managed by checkout and return operations.";
				case "loan_already_returned" -> "This loan has already been returned.";
				case "member_account_linked" -> "A member linked to a login account cannot be deleted.";
				case "book_copy_reserved" -> "This copy is currently held for a reservation.";
				case "reservation_already_active" -> "This member already has an active reservation for this title.";
				case "book_available_for_checkout" -> "A copy is available for immediate checkout.";
				case "reservation_not_active" -> "This reservation is no longer active.";
				case "reservation_not_ready" -> "This reservation does not currently have a held copy.";
				case "reservation_copy_unavailable" -> "The held copy is no longer available.";
				case "member_required" -> "A member must be selected for this operation.";
				default -> "The request conflicts with existing data.";
			};
			default -> "The request could not be completed.";
		};
		return ResponseEntity.status(status).body(Map.of("error", error, "message", message));
	}
}
