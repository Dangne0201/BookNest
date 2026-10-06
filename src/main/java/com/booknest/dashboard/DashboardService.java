package com.booknest.dashboard;

import java.time.LocalDate;
import java.util.List;

import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.loan.LoanRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private final BookCopyRepository bookCopyRepository;
	private final LoanRepository loanRepository;
	private final ReservationRepository reservationRepository;

	public DashboardService(
			BookCopyRepository bookCopyRepository,
			LoanRepository loanRepository,
			ReservationRepository reservationRepository
	) {
		this.bookCopyRepository = bookCopyRepository;
		this.loanRepository = loanRepository;
		this.reservationRepository = reservationRepository;
	}

	@Transactional(readOnly = true)
	public DashboardSummary getSummary() {
		return new DashboardSummary(
				bookCopyRepository.countByStatus(BookCopy.Status.AVAILABLE),
				loanRepository.countByReturnDateIsNull(),
				loanRepository.countByReturnDateIsNullAndDueDateBefore(LocalDate.now()),
				reservationRepository.countByStatusIn(List.of(Reservation.Status.WAITING, Reservation.Status.HELD))
		);
	}
}
