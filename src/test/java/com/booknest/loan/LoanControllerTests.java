package com.booknest.loan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.account.AccountService;
import com.booknest.account.RegistrationRequest;
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationRepository;
import com.booknest.reservation.ReservationRequest;
import com.booknest.reservation.ReservationService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
class LoanControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private StaffAccountRepository staffAccountRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private AccountService accountService;

	@Autowired
	private ReservationService reservationService;

	private long memberId;
	private long copyId;

	@BeforeEach
	void prepareSharedRecords() {
		loanRepository.deleteAll();
		bookCopyRepository.deleteAll();
		bookRepository.deleteAll();
		memberRepository.deleteAll();
		ensureStaff("staff-a");
		ensureStaff("staff-b");

		memberId = memberRepository.save(new Member("Loan Test Member", null, null, null)).getId();
		Book book = bookRepository.save(new Book("Loan Test Book", "Author", null, null, null, null));
		copyId = bookCopyRepository.save(new BookCopy(book, BookCopy.Status.AVAILABLE)).getId();
	}

	@Test
	void staffCanCheckoutAndReturnAndTheLoanDatesAndActorsAreRecorded() throws Exception {
		MvcResult checkout = mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(memberId, copyId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.copyId").value(copyId))
				.andExpect(jsonPath("$.checkedOutBy").value("staff-a"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.overdue").value(false))
				.andReturn();
		long loanId = responseId(checkout);
		LocalDate checkoutDate = LocalDate.parse(JsonPath.read(checkout.getResponse().getContentAsString(),
				"$.checkoutDate").toString());
		LocalDate dueDate = LocalDate.parse(JsonPath.read(checkout.getResponse().getContentAsString(),
				"$.dueDate").toString());
		assertEquals(14, ChronoUnit.DAYS.between(checkoutDate, dueDate));
		assertTrue(!checkoutDate.isAfter(LocalDate.now()));

		assertEquals(BookCopy.Status.ON_LOAN, bookCopyRepository.findById(copyId).orElseThrow().getStatus());
		mockMvc.perform(get("/api/loans").with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(loanId))
				.andExpect(jsonPath("$.items[0].memberName").value("Loan Test Member"))
				.andExpect(jsonPath("$.items[0].bookTitle").value("Loan Test Book"));

		mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
						.with(user("staff-b").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$.returnedBy").value("staff-b"))
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.overdue").value(false));

		assertEquals(BookCopy.Status.AVAILABLE, bookCopyRepository.findById(copyId).orElseThrow().getStatus());
		assertEquals(1, loanRepository.count());
	}

	@Test
	void rejectsUnavailableCopiesMissingReferencesAndRepeatedReturn() throws Exception {
		mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(999999, copyId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("member_not_found"));

		mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(memberId, 999999)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("book_copy_not_found"));

		long loanId = checkout(memberId, copyId, "staff-a");
		mockMvc.perform(post("/api/loans")
						.with(user("staff-b").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(memberId, copyId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("book_copy_unavailable"));

		mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
						.with(user("staff-b").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("loan_already_returned"));
		mockMvc.perform(post("/api/loans/999999/return")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("loan_not_found"));
	}

	@Test
	void derivesOverdueStateAndPreservesMemberAndCopyLoanHistory() throws Exception {
		Loan loan = loanRepository.saveAndFlush(new Loan(
				memberRepository.findById(memberId).orElseThrow(),
				bookCopyRepository.findById(copyId).orElseThrow(),
				LocalDate.now().minusDays(20),
				LocalDate.now().minusDays(6),
				staffAccountRepository.findByUsername("staff-a").orElseThrow()
		));
		BookCopy copy = bookCopyRepository.findById(copyId).orElseThrow();
		copy.updateStatus(BookCopy.Status.ON_LOAN);
		bookCopyRepository.save(copy);

		mockMvc.perform(get("/api/loans/{loanId}", loan.getId()).with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.overdue").value(true));

		mockMvc.perform(put("/api/books/{bookId}/copies/{copyId}",
						loan.getBookCopy().getBook().getId(), copyId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"AVAILABLE"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("book_copy_on_loan"));

		mockMvc.perform(post("/api/loans/{loanId}/return", loan.getId())
						.with(user("staff-b").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.overdue").value(false));

		mockMvc.perform(delete("/api/members/{memberId}", memberId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("member_has_loan_history"));

		mockMvc.perform(delete("/api/books/{bookId}/copies/{copyId}", loan.getBookCopy().getBook().getId(), copyId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("book_copy_has_loan_history"));

		assertTrue(memberRepository.existsById(memberId));
		assertTrue(bookCopyRepository.existsById(copyId));
		assertEquals(1, loanRepository.count());
	}

	@Test
	void staffCanRenewAnEligibleLoanOnceAndTheRenewalActorIsRecorded() throws Exception {
		long loanId = checkout(memberId, copyId, "staff-a");
		LocalDate originalDueDate = loanRepository.findById(loanId).orElseThrow().getDueDate();

		mockMvc.perform(get("/api/loans")
						.with(user("staff-a").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].renewalEligible").value(true));

		mockMvc.perform(post("/api/loans/{loanId}/renew", loanId)
						.with(user("staff-b").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.renewed").value(true))
				.andExpect(jsonPath("$.renewedBy").value("staff-b"))
				.andExpect(jsonPath("$.renewedAt").isNotEmpty())
				.andExpect(jsonPath("$.renewalEligible").value(false))
				.andExpect(jsonPath("$.dueDate").value(originalDueDate.plusDays(14).toString()));

		Loan renewedLoan = loanRepository.findWithDetailsById(loanId).orElseThrow();
		assertEquals(originalDueDate.plusDays(14), renewedLoan.getDueDate());
		assertEquals("staff-b", renewedLoan.getRenewedBy().getUsername());

		mockMvc.perform(post("/api/loans/{loanId}/renew", loanId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("loan_already_renewed"));
		assertEquals(originalDueDate.plusDays(14), loanRepository.findById(loanId).orElseThrow().getDueDate());
		assertEquals(BookCopy.Status.ON_LOAN, bookCopyRepository.findById(copyId).orElseThrow().getStatus());
	}

	@Test
	void rejectsRenewalForOverdueAndReturnedLoans() throws Exception {
		LocalDate today = LocalDate.now();
		Loan overdueLoan = loanRepository.saveAndFlush(new Loan(
				memberRepository.findById(memberId).orElseThrow(),
				bookCopyRepository.findById(copyId).orElseThrow(),
				today.minusDays(20),
				today.minusDays(6),
				staffAccountRepository.findByUsername("staff-a").orElseThrow()
		));
		BookCopy copy = bookCopyRepository.findById(copyId).orElseThrow();
		copy.updateStatus(BookCopy.Status.ON_LOAN);
		bookCopyRepository.save(copy);

		mockMvc.perform(post("/api/loans/{loanId}/renew", overdueLoan.getId())
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("loan_overdue"));
		assertEquals(today.minusDays(6), loanRepository.findById(overdueLoan.getId()).orElseThrow().getDueDate());

		mockMvc.perform(post("/api/loans/{loanId}/return", overdueLoan.getId())
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/loans/{loanId}/renew", overdueLoan.getId())
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("loan_not_active"));
	}

	@Test
	void activeWaitingOrHeldReservationsBlockRenewal() throws Exception {
		long loanId = checkout(memberId, copyId, "staff-a");
		Book book = bookRepository.findAll().get(0);
		Member queuedMember = memberRepository.saveAndFlush(new Member("Queued Reader", null, null, null));
		reservationRepository.saveAndFlush(new Reservation(queuedMember, book));

		mockMvc.perform(post("/api/loans/{loanId}/renew", loanId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("loan_has_reservation_queue"));
		mockMvc.perform(get("/api/loans")
						.with(user("staff-a").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].renewalEligible").value(false));

		reservationRepository.deleteAll();
		BookCopy heldCopy = bookCopyRepository.saveAndFlush(new BookCopy(book, BookCopy.Status.ON_HOLD));
		Reservation heldReservation = new Reservation(queuedMember, book);
		heldReservation.hold(heldCopy);
		reservationRepository.saveAndFlush(heldReservation);

		mockMvc.perform(post("/api/loans/{loanId}/renew", loanId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("loan_has_reservation_queue"));
		assertNull(loanRepository.findById(loanId).orElseThrow().getRenewedAt());
	}

	@Test
	void searchesFiltersSortsAndPaginatesLoansWithinPatronOwnership() throws Exception {
		long activeLoanId = checkout(memberId, copyId, "staff-a");
		Book secondBook = bookRepository.save(new Book("Another Loan Title", "Author", null, null, null, null));
		BookCopy secondCopy = bookCopyRepository.save(new BookCopy(secondBook, BookCopy.Status.AVAILABLE));
		long returnedLoanId = checkout(memberId, secondCopy.getId(), "staff-a");
		mockMvc.perform(post("/api/loans/{loanId}/return", returnedLoanId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.param("q", "ANOTHER")
						.param("state", "RETURNED")
						.param("sort", "memberName")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(returnedLoanId))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/loans")
						.with(user("staff-b").roles("STAFF"))
						.param("state", "ACTIVE")
						.param("sort", "dueDate")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(activeLoanId))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));

		var patronA = accountService.registerPatron(new RegistrationRequest(
				"loan-reader-a", "reader-password", "Loan Reader A", null, null, null
		));
		var patronB = accountService.registerPatron(new RegistrationRequest(
				"loan-reader-b", "reader-password", "Loan Reader B", null, null, null
		));
		Book patronBookA = bookRepository.save(new Book("Patron A Loan", "Author", null, null, null, null));
		BookCopy patronCopyA = bookCopyRepository.save(new BookCopy(patronBookA, BookCopy.Status.AVAILABLE));
		long patronLoanId = checkout(patronA.memberId(), patronCopyA.getId(), "staff-a");
		Book patronBookB = bookRepository.save(new Book("Patron B Loan", "Author", null, null, null, null));
		BookCopy patronCopyB = bookCopyRepository.save(new BookCopy(patronBookB, BookCopy.Status.AVAILABLE));
		checkout(patronB.memberId(), patronCopyB.getId(), "staff-a");

		mockMvc.perform(get("/api/loans")
						.with(user("loan-reader-a").roles("PATRON")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.items[0].id").value(patronLoanId))
				.andExpect(jsonPath("$.items[0].memberName").value("Loan Reader A"));
	}

	@Test
	void rejectsInvalidLoanPagingSortingAndStateFilters() throws Exception {
		for (String query : new String[]{
				"?page=-1",
				"?size=101",
				"?sort=member.passwordHash",
				"?direction=sideways",
				"?state=ACTIVE%20OR%201%3D1"
		}) {
			mockMvc.perform(get("/api/loans" + query)
							.with(user("staff-a").roles("STAFF")))
					.andExpect(status().isBadRequest());
		}
	}

	@Test
	void patronCanRenewOnlyTheirOwnLoanAndWritesRequireCsrf() throws Exception {
		var patron = accountService.registerPatron(new RegistrationRequest(
				"renewing-patron", "reader-password", "Renewing Patron", null, null, null
		));
		var otherPatron = accountService.registerPatron(new RegistrationRequest(
				"other-patron", "reader-password", "Other Patron", null, null, null
		));
		long patronLoanId = checkout(patron.memberId(), copyId, "staff-a");

		mockMvc.perform(post("/api/loans/{loanId}/renew", patronLoanId)
						.with(user("other-patron").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isNotFound());
		assertEquals("renewing-patron",
				staffAccountRepository.findById(patron.id()).orElseThrow().getUsername());

		mockMvc.perform(post("/api/loans/{loanId}/renew", patronLoanId)
						.with(user("renewing-patron").roles("PATRON")))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/loans/{loanId}/renew", patronLoanId)
						.with(user("renewing-patron").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.renewedBy").value("renewing-patron"));

		mockMvc.perform(post("/api/loans/{loanId}/renew", patronLoanId)
						.with(user("other-patron").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isNotFound());
		assertEquals(StaffAccount.Role.PATRON,
				staffAccountRepository.findById(otherPatron.id()).orElseThrow().getRole());
	}

	@Test
	void serializesConcurrentCheckoutAttemptsForTheSameCopy() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> first = executor.submit(() -> checkoutAfter(start, "staff-a"));
			Future<Integer> second = executor.submit(() -> checkoutAfter(start, "staff-b"));
			start.countDown();

			int firstStatus = first.get(10, TimeUnit.SECONDS).intValue();
			int secondStatus = second.get(10, TimeUnit.SECONDS).intValue();
			assertTrue(
					(firstStatus == 201 && secondStatus == 409) || (firstStatus == 409 && secondStatus == 201),
					"Exactly one concurrent checkout must succeed."
			);
			assertEquals(1, loanRepository.count());
			assertEquals(BookCopy.Status.ON_LOAN, bookCopyRepository.findById(copyId).orElseThrow().getStatus());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void serializesRenewalAgainstNewReservationForTheSameTitle() throws Exception {
		long loanId = checkout(memberId, copyId, "staff-a");
		LocalDate originalDueDate = loanRepository.findById(loanId).orElseThrow().getDueDate();
		Member queuedMember = memberRepository.saveAndFlush(new Member("Concurrent Queue Reader", null, null, null));
		long bookId = bookRepository.findAll().get(0).getId();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> renewal = executor.submit(() -> renewAfter(start, loanId, "staff-a"));
			Future<?> reservation = executor.submit(() -> {
				start.await();
				reservationService.create(new ReservationRequest(bookId, queuedMember.getId()), "staff-b");
				return null;
			});
			start.countDown();

			int renewalStatus = renewal.get(10, TimeUnit.SECONDS).intValue();
			reservation.get(10, TimeUnit.SECONDS);
			assertTrue(renewalStatus == 200 || renewalStatus == 409);
			assertEquals(1, reservationRepository.count());
			Loan loan = loanRepository.findById(loanId).orElseThrow();
			if (renewalStatus == 409) {
				assertNull(loan.getRenewedAt());
				assertEquals(originalDueDate, loan.getDueDate());
			} else {
				assertTrue(loan.getRenewedAt() != null);
				assertEquals(originalDueDate.plusDays(14), loan.getDueDate());
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void serializesConcurrentRenewalAttempts() throws Exception {
		long loanId = checkout(memberId, copyId, "staff-a");
		LocalDate originalDueDate = loanRepository.findById(loanId).orElseThrow().getDueDate();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> first = executor.submit(() -> renewAfter(start, loanId, "staff-a"));
			Future<Integer> second = executor.submit(() -> renewAfter(start, loanId, "staff-b"));
			start.countDown();

			int firstStatus = first.get(10, TimeUnit.SECONDS).intValue();
			int secondStatus = second.get(10, TimeUnit.SECONDS).intValue();
			assertTrue(
					(firstStatus == 200 && secondStatus == 409) || (firstStatus == 409 && secondStatus == 200),
					"Exactly one concurrent renewal must succeed."
			);
			Loan loan = loanRepository.findWithDetailsById(loanId).orElseThrow();
			assertEquals(originalDueDate.plusDays(14), loan.getDueDate());
			assertTrue("staff-a".equals(loan.getRenewedBy().getUsername())
					|| "staff-b".equals(loan.getRenewedBy().getUsername()));
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void loanApiRequiresAuthenticationAndCsrfForWrites() throws Exception {
		mockMvc.perform(get("/api/loans"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(memberId, copyId)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberId":0,"copyId":1}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));
	}

	private int checkoutAfter(CountDownLatch start, String staff) throws Exception {
		start.await();
		return mockMvc.perform(post("/api/loans")
						.with(user(staff).roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(memberId, copyId)))
				.andReturn()
				.getResponse()
				.getStatus();
	}

	private int renewAfter(CountDownLatch start, long loanId, String staff) throws Exception {
		start.await();
		return mockMvc.perform(post("/api/loans/{loanId}/renew", loanId)
						.with(user(staff).roles("STAFF"))
						.with(csrf()))
				.andReturn()
				.getResponse()
				.getStatus();
	}

	private long checkout(long member, long copy, String staff) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/loans")
						.with(user(staff).roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest(member, copy)))
				.andExpect(status().isCreated())
				.andReturn();
		return responseId(result);
	}

	private static String checkoutRequest(long member, long copy) {
		return "{\"memberId\":" + member + ",\"copyId\":" + copy + "}";
	}

	private static long responseId(MvcResult result) throws Exception {
		String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();
		return Long.parseLong(id);
	}

	private void ensureStaff(String username) {
		if (!staffAccountRepository.existsByUsername(username)) {
			staffAccountRepository.save(new StaffAccount(username, "test-password-hash"));
		}
	}
}
