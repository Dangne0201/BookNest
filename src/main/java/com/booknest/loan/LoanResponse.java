package com.booknest.loan;

import java.time.LocalDate;

public record LoanResponse(
		Long id,
		Long memberId,
		String memberName,
		Long bookId,
		String bookTitle,
		Long copyId,
		LocalDate checkoutDate,
		LocalDate dueDate,
		LocalDate returnDate,
		String checkedOutBy,
		String returnedBy,
		boolean active,
		boolean overdue
) {
}
