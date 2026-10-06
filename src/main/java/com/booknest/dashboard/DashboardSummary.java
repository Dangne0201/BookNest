package com.booknest.dashboard;

public record DashboardSummary(
		long availableCopies,
		long activeLoans,
		long overdueLoans,
		long activeReservations
) {
}
