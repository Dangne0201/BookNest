package com.booknest.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(
		@NotNull @Positive Long bookId,
		@Positive Long memberId
) {
}
