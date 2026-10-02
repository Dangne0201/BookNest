package com.booknest.loan;

import jakarta.validation.constraints.Positive;

public record CheckoutRequest(
		@Positive Long memberId,
		@jakarta.validation.constraints.NotNull @Positive Long copyId
) {
}
