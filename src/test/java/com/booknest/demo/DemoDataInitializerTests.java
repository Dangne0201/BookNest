package com.booknest.demo;

import java.time.LocalDate;
import java.util.List;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.loan.Loan;
import com.booknest.loan.LoanRepository;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "booknest.demo-data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDataInitializerTests {

	@Autowired
	private DemoDataInitializer initializer;

	@Autowired
	private StaffAccountRepository accountRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository copyRepository;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private DemoSeedRunRepository seedRunRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void resetAndInitializeDemoData() {
		reservationRepository.deleteAll();
		loanRepository.deleteAll();
		copyRepository.deleteAll();
		bookRepository.deleteAll();
		memberRepository.deleteAll();
		accountRepository.deleteAll();
		seedRunRepository.deleteAll();
		initializer.initialize();
	}

	@Test
	void createsACompleteDemoDatasetWithValidRelationships() {
		StaffAccount staff = accountRepository.findByUsername("demo-staff").orElseThrow();
		StaffAccount patron = accountRepository.findByUsername("demo-patron").orElseThrow();
		StaffAccount secondPatron = accountRepository.findByUsername("demo-patron-two").orElseThrow();

		assertEquals(StaffAccount.Role.STAFF, staff.getRole());
		assertEquals(StaffAccount.Role.PATRON, patron.getRole());
		assertEquals(StaffAccount.Role.PATRON, secondPatron.getRole());
		assertFalse(accountRepository.existsByRole(StaffAccount.Role.ADMIN));
		assertTrue(passwordEncoder.matches("BookNestDemoStaff2026!", staff.getPasswordHash()));
		assertTrue(passwordEncoder.matches("BookNestDemoReader2026!", patron.getPasswordHash()));
		assertFalse(staff.getPasswordHash().contains("BookNestDemo"));
		assertNotNull(memberRepository.findByAccountUsername("demo-patron").orElseThrow());
		assertNotNull(memberRepository.findByAccountUsername("demo-patron-two").orElseThrow());

		List<Book> demoBooks = bookRepository.findAllByOrderByTitleAscIdAsc().stream()
				.filter(book -> book.getIsbn() != null
						&& List.of("9780135957059", "9780132350884", "9780547928227").contains(book.getIsbn()))
				.toList();
		assertEquals(3, demoBooks.size());
		assertEquals(9, demoBooks.stream()
				.mapToLong(book -> copyRepository.countByBookId(book.getId()))
				.sum());
		assertTrue(demoBooks.stream().anyMatch(book ->
				copyRepository.countByBookIdAndStatus(book.getId(), BookCopy.Status.AVAILABLE) > 0));
		assertTrue(demoBooks.stream().anyMatch(book ->
				copyRepository.countByBookIdAndStatus(book.getId(), BookCopy.Status.MAINTENANCE) > 0));
		assertTrue(demoBooks.stream().anyMatch(book ->
				copyRepository.countByBookIdAndStatus(book.getId(), BookCopy.Status.RETIRED) > 0));

		List<Loan> patronLoans = loanRepository.findAllByMemberAccountUsernameOrderByCheckoutDateDescIdDesc(
				"demo-patron"
		);
		List<Loan> secondPatronLoans = loanRepository.findAllByMemberAccountUsernameOrderByCheckoutDateDescIdDesc(
				"demo-patron-two"
		);
		assertEquals(1, patronLoans.size());
		assertNullReturnDate(patronLoans.get(0));
		assertTrue(patronLoans.get(0).getDueDate().isAfter(LocalDate.now()));
		assertEquals(1, secondPatronLoans.size());
		assertNullReturnDate(secondPatronLoans.get(0));
		assertTrue(secondPatronLoans.get(0).getDueDate().isBefore(LocalDate.now()));
		assertTrue(loanRepository.findAllByOrderByCheckoutDateDescIdDesc().stream()
				.anyMatch(loan -> loan.getReturnDate() != null && loan.getReturnedBy() != null));

		List<Reservation> reservations = reservationRepository.findAllByOrderByIdDesc();
		assertTrue(reservations.stream().anyMatch(reservation ->
				reservation.getStatus() == Reservation.Status.HELD && reservation.getCopy() != null
						&& reservation.getCopy().getStatus() == BookCopy.Status.ON_HOLD));
		assertTrue(reservations.stream().anyMatch(reservation ->
				reservation.getStatus() == Reservation.Status.WAITING));
		assertTrue(reservations.stream().anyMatch(reservation ->
				reservation.getStatus() == Reservation.Status.CANCELLED));
		assertTrue(reservations.stream().anyMatch(reservation ->
				reservation.getStatus() == Reservation.Status.FULFILLED));
	}

	@Test
	void repeatedInitializationDoesNotDuplicateRecordsOrResetUserData() {
		long accountsBefore = accountRepository.count();
		long membersBefore = memberRepository.count();
		long booksBefore = bookRepository.count();
		long copiesBefore = copyRepository.count();
		long loansBefore = loanRepository.count();
		long reservationsBefore = reservationRepository.count();
		Book demoBook = bookRepository.findAllByOrderByTitleAscIdAsc().stream()
				.filter(book -> "9780135957059".equals(book.getIsbn()))
				.findFirst()
				.orElseThrow();
		StaffAccount patron = accountRepository.findByUsername("demo-patron").orElseThrow();
		demoBook.updateDetails(
				"The Pragmatic Programmer",
				"David Thomas, Andrew Hunt",
				demoBook.getIsbn(),
				demoBook.getGenre(),
				demoBook.getPublicationYear(),
				"Preserve this user edit."
		);
		bookRepository.save(demoBook);
		patron.updatePasswordHash(passwordEncoder.encode("PersonalDemoPassword2026!"));
		accountRepository.save(patron);
		var patronMember = memberRepository.findByAccountUsername("demo-patron").orElseThrow();
		patronMember.updateDetails("Edited Demo Patron", null, null, null);
		memberRepository.save(patronMember);

		initializer.initialize();

		assertEquals(accountsBefore, accountRepository.count());
		assertEquals(membersBefore, memberRepository.count());
		assertEquals(booksBefore, bookRepository.count());
		assertEquals(copiesBefore, copyRepository.count());
		assertEquals(loansBefore, loanRepository.count());
		assertEquals(reservationsBefore, reservationRepository.count());
		assertEquals("Preserve this user edit.",
				bookRepository.findById(demoBook.getId()).orElseThrow().getDescription());
		assertTrue(passwordEncoder.matches(
				"PersonalDemoPassword2026!",
				accountRepository.findByUsername("demo-patron").orElseThrow().getPasswordHash()
		));
		assertEquals("Edited Demo Patron", memberRepository.findByAccountUsername("demo-patron")
				.orElseThrow().getFullName());
	}

	@Test
	void conflictingDemoUsernameIsReportedWithoutChangingTheExistingAccount() {
		StaffAccount existing = accountRepository.findByUsername("demo-staff").orElseThrow();
		existing.updateRole(StaffAccount.Role.PATRON);
		accountRepository.save(existing);
		seedRunRepository.deleteAll();

		assertThrows(IllegalStateException.class, initializer::initialize);
		assertEquals(StaffAccount.Role.PATRON,
				accountRepository.findByUsername("demo-staff").orElseThrow().getRole());
	}

	@Test
	void conflictingDemoIsbnIsReportedWithoutChangingTheExistingBook() {
		Book existing = bookRepository.findAllByOrderByTitleAscIdAsc().stream()
				.filter(book -> "9780135957059".equals(book.getIsbn()))
				.findFirst()
				.orElseThrow();
		existing.updateDetails(
				"User-owned title",
				existing.getAuthor(),
				existing.getIsbn(),
				existing.getGenre(),
				existing.getPublicationYear(),
				existing.getDescription()
		);
		bookRepository.save(existing);
		seedRunRepository.deleteAll();

		assertThrows(IllegalStateException.class, initializer::initialize);
		assertEquals("User-owned title",
				bookRepository.findById(existing.getId()).orElseThrow().getTitle());
	}

	private static void assertNullReturnDate(Loan loan) {
		org.junit.jupiter.api.Assertions.assertNull(loan.getReturnDate());
	}
}
