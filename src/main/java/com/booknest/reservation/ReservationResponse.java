package com.booknest.reservation;

import java.time.Instant;

public record ReservationResponse(
		long id,
		long bookId,
		String bookTitle,
		long memberId,
		String memberName,
		Reservation.Status status,
		Long queuePosition,
		Long copyId,
		Instant createdAt
) {
}
