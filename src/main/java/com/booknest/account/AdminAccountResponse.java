package com.booknest.account;

public record AdminAccountResponse(
		long id,
		String username,
		StaffAccount.Role role,
		boolean passwordChangeRequired,
		String fullName
) {
}
