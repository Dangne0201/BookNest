package com.booknest.member;

public record MemberResponse(
		Long id,
		String fullName,
		String email,
		String phone,
		String notes
) {
}
