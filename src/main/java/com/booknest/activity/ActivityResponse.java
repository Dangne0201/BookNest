package com.booknest.activity;

import java.time.Instant;

public record ActivityResponse(
		Long id,
		ActivityEventType type,
		Instant occurredAt,
		String actorUsername,
		Long memberId,
		String memberName,
		Long bookId,
		String bookTitle,
		Long copyId,
		Long loanId,
		Long reservationId
) {
}
