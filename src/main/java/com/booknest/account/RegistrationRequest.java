package com.booknest.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank
		@Size(min = 3, max = 50)
		String username,
		@NotBlank
		@Size(min = 8, max = 72)
		String password,
		@NotBlank
		@Size(max = 150)
		String fullName,
		@Email
		@Size(max = 254)
		String email,
		@Size(max = 30)
		String phone,
		@Size(max = 1000)
		String notes
) {
	public RegistrationRequest(String username, String password) {
		this(username, password, username, null, null, null);
	}
}
