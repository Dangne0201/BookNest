package com.booknest.loan;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.account.StaffAccount.Role;
import com.booknest.activity.ActivityEventType;
import com.booknest.activity.ActivityService;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.common.PageRequestFactory;
import com.booknest.common.PageResponse;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanService {

	private static final long LOAN_PERIOD_DAYS = 14;

	private final LoanRepository loanRepository;
	private final BookCopyRepository bookCopyRepository;
	private final BookRepository bookRepository;
	private final MemberRepository memberRepository;
	private final StaffAccountRepository staffAccountRepository;
	private final ReservationService reservationService;
	private final ActivityService activityService;

	public LoanService(
			LoanRepository loanRepository,
			BookCopyRepository bookCopyRepository,
			BookRepository bookRepository,
			MemberRepository memberRepository,
			StaffAccountRepository staffAccountRepository,
			ReservationService reservationService,
			ActivityService activityService
	) {
		this.loanRepository = loanRepository;
		this.bookCopyRepository = bookCopyRepository;
		this.bookRepository = bookRepository;
		this.memberRepository = memberRepository;
		this.staffAccountRepository = staffAccountRepository;
		this.reservationService = reservationService;
		this.activityService = activityService;
	}

	@Transactional(readOnly = true)
	public PageResponse<LoanResponse> findAll(
			String username,
			String query,
			String state,
			int page,
			int size,
			String sort,
			String direction
	) {
		LocalDate today = LocalDate.now();
		StaffAccount account = getStaff(username);
		String normalizedState = state == null || state.isBlank() ? null : state.toUpperCase(java.util.Locale.ROOT);
		if (normalizedState != null
				&& !normalizedState.equals("ACTIVE")
				&& !normalizedState.equals("OVERDUE")
				&& !normalizedState.equals("RETURNED")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_loan_state");
		}
		PageRequest pageable = PageRequestFactory.create(
				page,
				size,
				sort,
				direction,
				java.util.Map.of(
						"checkoutDate", "checkoutDate",
						"dueDate", "dueDate",
						"returnDate", "returnDate",
						"bookTitle", "bookCopy.book.title",
						"memberName", "member.fullName"
				),
				"checkoutDate",
				"desc"
		);
		Page<Loan> loanPage = loanRepository.search(
				account.getRole() == Role.PATRON ? account.getId() : null,
				query == null ? "" : query.trim(),
				normalizedState,
				today,
				pageable
		);
		List<Long> bookIds = loanPage.getContent().stream()
				.map(loan -> loan.getBookCopy().getBook().getId())
				.distinct()
				.toList();
		Set<Long> booksWithActiveReservations = reservationService.findBookIdsWithActiveReservations(bookIds);
		return PageResponse.from(loanPage.map(loan -> toResponse(loan, today, booksWithActiveReservations)));
	}

	@Transactional(readOnly = true)
	public LoanResponse findById(long loanId, String username) {
		Loan loan = loanRepository.findWithDetailsById(loanId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "loan_not_found"));
		StaffAccount account = getStaff(username);
		if (account.getRole() == Role.PATRON
				&& (loan.getMember().getAccount() == null
				|| !loan.getMember().getAccount().getUsername().equals(username))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "loan_not_found");
		}
		return toResponse(loan, LocalDate.now(),
				reservationService.findBookIdsWithActiveReservations(List.of(loan.getBookCopy().getBook().getId())));
	}

	@Transactional
	public LoanResponse checkout(CheckoutRequest request, String username) {
		BookCopy initialCopy = bookCopyRepository.findById(request.copyId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_copy_not_found"));
		bookRepository.findByIdForUpdate(initialCopy.getBook().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
		BookCopy copy = bookCopyRepository.findByIdForUpdate(request.copyId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_copy_not_found"));
		StaffAccount actor = getStaff(username);
		if (actor.getRole() != Role.PATRON && request.memberId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member_required");
		}
		Member member = actor.getRole() == Role.PATRON
				? memberRepository.findByAccountUsername(username)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "patron_profile_missing"))
				: memberRepository.findById(request.memberId())
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member_not_found"));

		if (copy.getStatus() != BookCopy.Status.AVAILABLE || loanRepository.existsByActiveCopyId(copy.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_copy_unavailable");
		}

		LocalDate checkoutDate = LocalDate.now();
		Loan loan = new Loan(member, copy, checkoutDate, checkoutDate.plusDays(LOAN_PERIOD_DAYS), actor);
		copy.updateStatus(BookCopy.Status.ON_LOAN);
		Loan savedLoan = loanRepository.save(loan);
		activityService.recordLoanEvent(ActivityEventType.LOAN_CHECKED_OUT, savedLoan, actor.getUsername());
		return toResponse(
				savedLoan,
				checkoutDate,
				reservationService.findBookIdsWithActiveReservations(List.of(copy.getBook().getId()))
		);
	}

	@Transactional
	public LoanResponse checkoutReservation(long reservationId, String username) {
		Reservation reservation = reservationService.getHeldForCheckout(reservationId, username);
		BookCopy copy = bookCopyRepository.findByIdForUpdate(reservation.getCopy().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "reservation_copy_missing"));
		if (copy.getStatus() != BookCopy.Status.ON_HOLD
				|| loanRepository.existsByActiveCopyId(copy.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "reservation_copy_unavailable");
		}

		StaffAccount patron = getStaff(username);
		LocalDate checkoutDate = LocalDate.now();
		Loan loan = new Loan(
				reservation.getMember(),
				copy,
				checkoutDate,
				checkoutDate.plusDays(LOAN_PERIOD_DAYS),
				patron
		);
		copy.updateStatus(BookCopy.Status.ON_LOAN);
		reservationService.markFulfilled(reservation, username);
		Loan savedLoan = loanRepository.save(loan);
		activityService.recordLoanEvent(ActivityEventType.LOAN_CHECKED_OUT, savedLoan, username);
		return toResponse(
				savedLoan,
				checkoutDate,
				reservationService.findBookIdsWithActiveReservations(List.of(copy.getBook().getId()))
		);
	}

	@Transactional
	public LoanResponse returnLoan(long loanId, String staffUsername) {
		Loan loan = loanRepository.findByIdForUpdate(loanId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "loan_not_found"));
		if (loan.getReturnDate() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "loan_already_returned");
		}

		BookCopy initialCopy = bookCopyRepository.findById(loan.getBookCopy().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "loan_copy_missing"));
		bookRepository.findByIdForUpdate(initialCopy.getBook().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "loan_book_missing"));
		BookCopy copy = bookCopyRepository.findByIdForUpdate(loan.getBookCopy().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "loan_copy_missing"));
		if (copy.getStatus() != BookCopy.Status.ON_LOAN
				|| !loanRepository.existsByActiveCopyId(copy.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "loan_copy_state_conflict");
		}

		StaffAccount staff = getStaff(staffUsername);
		LocalDate returnDate = LocalDate.now();
		loan.returnOn(returnDate, staff);
		Loan savedLoan = loanRepository.save(loan);
		activityService.recordLoanEvent(ActivityEventType.LOAN_RETURNED, savedLoan, staff.getUsername());
		reservationService.holdNext(initialCopy.getBook().getId(), copy, staff.getUsername());
		return toResponse(
				savedLoan,
				returnDate,
				reservationService.findBookIdsWithActiveReservations(List.of(initialCopy.getBook().getId()))
		);
	}

	@Transactional
	public LoanResponse renewLoan(long loanId, String username) {
		Loan loan = loanRepository.findByIdForUpdate(loanId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "loan_not_found"));
		StaffAccount actor = getStaff(username);
		if (actor.getRole() == Role.PATRON
				&& (loan.getMember().getAccount() == null
				|| !loan.getMember().getAccount().getUsername().equals(username))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "loan_not_found");
		}

		if (loan.getReturnDate() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "loan_not_active");
		}
		LocalDate today = LocalDate.now();
		if (loan.getDueDate().isBefore(today)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "loan_overdue");
		}
		if (loan.getRenewedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "loan_already_renewed");
		}

		long bookId = loan.getBookCopy().getBook().getId();
		bookRepository.findByIdForUpdate(bookId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "loan_book_missing"));
		if (reservationService.hasActiveReservations(bookId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "loan_has_reservation_queue");
		}

		loan.renewUntil(loan.getDueDate().plusDays(LOAN_PERIOD_DAYS), actor);
		Loan savedLoan = loanRepository.save(loan);
		activityService.recordLoanEvent(ActivityEventType.LOAN_RENEWED, savedLoan, actor.getUsername());
		return toResponse(
				savedLoan,
				today,
				reservationService.findBookIdsWithActiveReservations(List.of(bookId))
		);
	}

	private StaffAccount getStaff(String username) {
		return staffAccountRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
	}

	private static LoanResponse toResponse(Loan loan, LocalDate today, Set<Long> booksWithActiveReservations) {
		boolean active = loan.getReturnDate() == null;
		boolean overdue = active && loan.getDueDate().isBefore(today);
		boolean renewed = loan.getRenewedAt() != null;
		long bookId = loan.getBookCopy().getBook().getId();
		return new LoanResponse(
				loan.getId(),
				loan.getMember().getId(),
				loan.getMember().getFullName(),
				loan.getBookCopy().getBook().getId(),
				loan.getBookCopy().getBook().getTitle(),
				loan.getBookCopy().getId(),
				loan.getCheckoutDate(),
				loan.getDueDate(),
				loan.getReturnDate(),
				loan.getCheckedOutBy().getUsername(),
				loan.getReturnedBy() == null ? null : loan.getReturnedBy().getUsername(),
				active,
				overdue,
				renewed,
				loan.getRenewedAt(),
				loan.getRenewedBy() == null ? null : loan.getRenewedBy().getUsername(),
				active && !overdue && !renewed && !booksWithActiveReservations.contains(bookId)
		);
	}
}
