package com.booknest.loan;

import java.time.LocalDate;
import java.util.List;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.account.StaffAccount.Role;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationService;
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

	public LoanService(
			LoanRepository loanRepository,
			BookCopyRepository bookCopyRepository,
			BookRepository bookRepository,
			MemberRepository memberRepository,
			StaffAccountRepository staffAccountRepository,
			ReservationService reservationService
	) {
		this.loanRepository = loanRepository;
		this.bookCopyRepository = bookCopyRepository;
		this.bookRepository = bookRepository;
		this.memberRepository = memberRepository;
		this.staffAccountRepository = staffAccountRepository;
		this.reservationService = reservationService;
	}

	@Transactional(readOnly = true)
	public List<LoanResponse> findAll(String username) {
		LocalDate today = LocalDate.now();
		StaffAccount account = getStaff(username);
		List<Loan> result = account.getRole() == Role.PATRON
				? loanRepository.findAllByMemberAccountUsernameOrderByCheckoutDateDescIdDesc(username)
				: loanRepository.findAllByOrderByCheckoutDateDescIdDesc();
		return result.stream()
				.map(loan -> toResponse(loan, today))
				.toList();
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
		return toResponse(loan, LocalDate.now());
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
		return toResponse(loanRepository.save(loan), checkoutDate);
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
		reservationService.markFulfilled(reservation);
		return toResponse(loanRepository.save(loan), checkoutDate);
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
		reservationService.holdNext(initialCopy.getBook().getId(), copy);
		return toResponse(loanRepository.save(loan), returnDate);
	}

	private StaffAccount getStaff(String username) {
		return staffAccountRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
	}

	private static LoanResponse toResponse(Loan loan, LocalDate today) {
		boolean active = loan.getReturnDate() == null;
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
				active && loan.getDueDate().isBefore(today)
		);
	}
}
