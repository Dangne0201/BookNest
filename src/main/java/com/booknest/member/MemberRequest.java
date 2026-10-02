package com.booknest.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberRequest(
		@NotBlank @Size(max = 150) String fullName,
		@Email @Size(max = 254) String email,
		@Size(max = 30) String phone,
		@Size(max = 1000) String notes
) {

	public MemberRequest {
		email = trimToNull(email);
		phone = trimToNull(phone);
		notes = trimToNull(notes);
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
