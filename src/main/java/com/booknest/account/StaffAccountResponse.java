package com.booknest.account;

public record StaffAccountResponse(
		Long id,
		String username,
		StaffAccount.Role role,
		Long memberId,
		boolean passwordChangeRequired
) {
}
