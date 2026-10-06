package com.booknest.demo;

import java.time.LocalDate;
import java.util.List;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccount.Role;
import com.booknest.account.StaffAccountRepository;
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.loan.Loan;
import com.booknest.loan.LoanRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "booknest.demo-data", name = "enabled", havingValue = "true")
@Transactional
public class DemoDataInitializer implements ApplicationRunner {

	private static final String SEED_KEY = "booknest-local-demo-v1";
	private static final String STAFF_USERNAME = "demo-staff";
	private static final String STAFF_PASSWORD = "BookNestDemoStaff2026!";
	private static final String PATRON_USERNAME = "demo-patron";
	private static final String PATRON_PASSWORD = "BookNestDemoReader2026!";
	private static final String SECOND_PATRON_USERNAME = "demo-patron-two";
	private static final String SECOND_PATRON_PASSWORD = "BookNestDemoReaderTwo2026!";

	private final StaffAccountRepository accountRepository;
	private final MemberRepository memberRepository;
	private final BookRepository bookRepository;
	private final BookCopyRepository copyRepository;
	private final LoanRepository loanRepository;
	private final ReservationRepository reservationRepository;
	private final DemoSeedRunRepository seedRunRepository;
	private final PasswordEncoder passwordEncoder;

	public DemoDataInitializer(
			StaffAccountRepository accountRepository,
			MemberRepository memberRepository,
			BookRepository bookRepository,
			BookCopyRepository copyRepository,
			LoanRepository loanRepository,
			ReservationRepository reservationRepository,
			DemoSeedRunRepository seedRunRepository,
			PasswordEncoder passwordEncoder
	) {
		this.accountRepository = accountRepository;
		this.memberRepository = memberRepository;
		this.bookRepository = bookRepository;
		this.copyRepository = copyRepository;
		this.loanRepository = loanRepository;
		this.reservationRepository = reservationRepository;
		this.seedRunRepository = seedRunRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		initialize();
	}

	public void initialize() {
		if (seedRunRepository.existsById(SEED_KEY)) {
			return;
		}

		StaffAccount staff = ensureAccount(STAFF_USERNAME, STAFF_PASSWORD, Role.STAFF);
		StaffAccount patron = ensureAccount(PATRON_USERNAME, PATRON_PASSWORD, Role.PATRON);
		StaffAccount secondPatron = ensureAccount(
				SECOND_PATRON_USERNAME,
				SECOND_PATRON_PASSWORD,
				Role.PATRON
		);

		Member patronMember = ensureLinkedMember(patron, "Demo Patron", "demo.patron@booknest.local");
		Member secondPatronMember = ensureLinkedMember(
				secondPatron,
				"Demo Patron Two",
				"demo.patron.two@booknest.local"
		);
		Member walkInMember = ensureWalkInMember();

		Book lanternLibrary = ensureBook(
				"9780135957059",
				"The Pragmatic Programmer",
				"David Thomas, Andrew Hunt",
				"Technology",
				2019,
				"Sample catalog record for exploring shared inventory."
		);
		Book smallSystems = ensureBook(
				"9780132350884",
				"Clean Code",
				"Robert C. Martin",
				"Technology",
				2008,
				"Sample catalog record with an active reservation queue."
		);
		Book mapsOfTomorrow = ensureBook(
				"9780547928227",
				"The Hobbit",
				"J. R. R. Tolkien",
				"Fantasy",
				2012,
				"Sample catalog record with available copies and reservation history."
		);

		List<BookCopy> lanternCopies = ensureCopies(lanternLibrary, BookCopy.Status.AVAILABLE,
				BookCopy.Status.ON_LOAN, BookCopy.Status.MAINTENANCE, BookCopy.Status.RETIRED);
		List<BookCopy> systemsCopies = ensureCopies(smallSystems, BookCopy.Status.ON_LOAN,
				BookCopy.Status.ON_HOLD, BookCopy.Status.MAINTENANCE);
		List<BookCopy> mapsCopies = ensureCopies(mapsOfTomorrow,
				BookCopy.Status.AVAILABLE, BookCopy.Status.AVAILABLE);

		LocalDate today = LocalDate.now();
		ensureLoan(patronMember, lanternCopies.get(1), today.minusDays(3), today.plusDays(11),
				null, patron, null);
		ensureLoan(secondPatronMember, systemsCopies.get(0), today.minusDays(21), today.minusDays(7),
				null, secondPatron, null);
		ensureLoan(walkInMember, lanternCopies.get(0), today.minusDays(20), today.minusDays(6),
				today.minusDays(10), staff, staff);

		ensureReservation(patronMember, smallSystems, systemsCopies.get(1), Reservation.Status.HELD);
		ensureReservation(secondPatronMember, smallSystems, null, Reservation.Status.WAITING);
		ensureReservation(walkInMember, lanternLibrary, null, Reservation.Status.CANCELLED);
		ensureReservation(secondPatronMember, mapsOfTomorrow, null, Reservation.Status.FULFILLED);
		seedRunRepository.save(new DemoSeedRun(SEED_KEY));
	}

	private StaffAccount ensureAccount(String username, String rawPassword, Role role) {
		return accountRepository.findByUsername(username).map(existing -> {
			if (existing.getRole() != role
					|| !passwordEncoder.matches(rawPassword, existing.getPasswordHash())
					|| existing.isPasswordChangeRequired()) {
				throw new IllegalStateException(
						"Reserved demo account '" + username
								+ "' already exists with conflicting credentials or role; it was not changed."
				);
			}
			return existing;
		}).orElseGet(() -> accountRepository.save(new StaffAccount(
				username,
				passwordEncoder.encode(rawPassword),
				role,
				false
		)));
	}

	private Member ensureLinkedMember(StaffAccount account, String fullName, String email) {
		return memberRepository.findByAccountUsername(account.getUsername()).orElseGet(() -> {
			Member member = new Member(fullName, email, null, "Local demo profile.");
			account.linkMember(member);
			return memberRepository.save(member);
		});
	}

	private Member ensureWalkInMember() {
		String email = "demo.walk-in@booknest.local";
		return memberRepository.findAllByOrderByFullNameAscIdAsc().stream()
				.filter(member -> email.equals(member.getEmail()))
				.findFirst()
				.map(existing -> {
					if (existing.getAccount() != null) {
						throw new IllegalStateException(
								"Reserved demo member email '" + email + "' is linked to an account."
						);
					}
					return existing;
				})
				.orElseGet(() -> memberRepository.save(new Member(
						"Demo Walk-in Reader",
						email,
						"555-0103",
						"Sample member record without a login account."
				)));
	}

	private Book ensureBook(
			String isbn,
			String title,
			String author,
			String genre,
			int publicationYear,
			String description
	) {
		return bookRepository.findAllByOrderByTitleAscIdAsc().stream()
				.filter(book -> isbn.equals(book.getIsbn()))
				.findFirst()
				.map(existing -> {
					if (!title.equals(existing.getTitle())) {
						throw new IllegalStateException(
								"Reserved demo ISBN '" + isbn + "' belongs to a different title; it was not changed."
						);
					}
					return existing;
				})
				.orElseGet(() -> bookRepository.save(new Book(
						title,
						author,
						isbn,
						genre,
						publicationYear,
						description
				)));
	}

	private List<BookCopy> ensureCopies(Book book, BookCopy.Status... initialStatuses) {
		List<BookCopy> copies = copyRepository.findAllByBookIdOrderByIdAsc(book.getId());
		for (int index = copies.size(); index < initialStatuses.length; index++) {
			copies.add(copyRepository.save(new BookCopy(book, initialStatuses[index])));
		}
		return copies;
	}

	private void ensureLoan(
			Member member,
			BookCopy copy,
			LocalDate checkoutDate,
			LocalDate dueDate,
			LocalDate returnDate,
			StaffAccount checkedOutBy,
			StaffAccount returnedBy
	) {
		boolean exists = loanRepository.findAllByOrderByCheckoutDateDescIdDesc().stream()
				.anyMatch(loan -> loan.getMember().getId().equals(member.getId())
						&& loan.getBookCopy().getId().equals(copy.getId()));
		if (exists) {
			return;
		}
		BookCopy.Status requiredStatus = returnDate == null
				? BookCopy.Status.ON_LOAN
				: BookCopy.Status.AVAILABLE;
		requireCopyStatus(copy, requiredStatus);

		Loan loan = new Loan(member, copy, checkoutDate, dueDate, checkedOutBy);
		if (returnDate != null) {
			loan.returnOn(returnDate, returnedBy);
		}
		loanRepository.save(loan);
	}

	private void ensureReservation(
			Member member,
			Book book,
			BookCopy copy,
			Reservation.Status status
	) {
		boolean exists = reservationRepository.findAllByOrderByIdDesc().stream()
				.anyMatch(reservation -> reservation.getMember().getId().equals(member.getId())
						&& reservation.getBook().getId().equals(book.getId()));
		if (exists) {
			return;
		}
		if (status == Reservation.Status.HELD) {
			if (copy == null) {
				throw new IllegalArgumentException("A held demo reservation requires a copy.");
			}
			requireCopyStatus(copy, BookCopy.Status.ON_HOLD);
		}
		if (status == Reservation.Status.WAITING
				&& copyRepository.countByBookIdAndStatus(book.getId(), BookCopy.Status.AVAILABLE) > 0) {
			throw new IllegalStateException("A waiting demo reservation cannot have an available copy.");
		}

		Reservation reservation = new Reservation(member, book);
		switch (status) {
			case HELD -> reservation.hold(copy);
			case CANCELLED -> reservation.cancel();
			case FULFILLED -> reservation.fulfill();
			case WAITING -> {
			}
		}
		reservationRepository.save(reservation);
	}

	private static void requireCopyStatus(BookCopy copy, BookCopy.Status expected) {
		if (copy.getStatus() != expected) {
			throw new IllegalStateException(
					"Demo copy " + copy.getId() + " must be " + expected
							+ " before its sample activity can be initialized."
			);
		}
	}
}
