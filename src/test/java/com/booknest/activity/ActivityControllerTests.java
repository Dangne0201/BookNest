package com.booknest.activity;

import com.booknest.account.AccountService;
import com.booknest.account.RegistrationRequest;
import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.loan.LoanRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.Reservation;
import com.booknest.reservation.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
class ActivityControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private StaffAccountRepository staffAccountRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ActivityEventRepository activityEventRepository;

	@Autowired
	private ActivityService activityService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void clearRecords() {
		activityEventRepository.deleteAll();
		reservationRepository.deleteAll();
		loanRepository.deleteAll();
		bookCopyRepository.deleteAll();
		bookRepository.deleteAll();
		memberRepository.deleteAll();
		staffAccountRepository.deleteAll();
	}

	@Test
	void recordsLifecycleHistoryAndScopesPatronActivity() throws Exception {
		ensureStaff("staff-a");
		var patronA = accountService.registerPatron(new RegistrationRequest(
				"reader-a", "reader-password", "Reader A", null, null, null
		));
		var patronB = accountService.registerPatron(new RegistrationRequest(
				"reader-b", "reader-password", "Reader B", null, null, null
		));
		Book book = bookRepository.saveAndFlush(new Book("Activity Test Book", "History Author", null, null, null, null));
		BookCopy copy = bookCopyRepository.saveAndFlush(new BookCopy(book, BookCopy.Status.AVAILABLE));

		String checkoutRequest = """
				{"memberId":%d,"copyId":%d}
				""".formatted(patronA.memberId(), copy.getId());
		MvcResult checkout = mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest))
				.andExpect(status().isCreated())
				.andReturn();
		long loanId = responseId(checkout);
		assertEquals(1, activityEventRepository.count());

		mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutRequest))
				.andExpect(status().isConflict());
		assertEquals(1, activityEventRepository.count());

		MvcResult reservationResultB = mockMvc.perform(post("/api/reservations")
						.with(user("reader-b").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bookId":%d}
								""".formatted(book.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		long reservationB = responseId(reservationResultB);

		mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/reservations/{reservationId}/checkout", reservationB)
						.with(user("reader-b").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isCreated());

		MvcResult reservationResultA = mockMvc.perform(post("/api/reservations")
						.with(user("reader-a").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bookId":%d}
								""".formatted(book.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		long reservationA = responseId(reservationResultA);
		mockMvc.perform(delete("/api/reservations/{reservationId}", reservationA)
						.with(user("reader-a").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isNoContent());

		Book renewalBook = bookRepository.saveAndFlush(new Book(
				"Renewal Activity Book", "Renewal Author", null, null, null, null
		));
		BookCopy renewalCopy = bookCopyRepository.saveAndFlush(
				new BookCopy(renewalBook, BookCopy.Status.AVAILABLE)
		);
		MvcResult renewalCheckout = mockMvc.perform(post("/api/loans")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberId":%d,"copyId":%d}
								""".formatted(patronA.memberId(), renewalCopy.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		long renewalLoanId = responseId(renewalCheckout);
		mockMvc.perform(post("/api/loans/{loanId}/renew", renewalLoanId)
						.with(user("reader-a").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.renewed").value(true));

		mockMvc.perform(get("/api/activity")
						.with(user("staff-a").roles("STAFF"))
						.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(10))
				.andExpect(jsonPath("$.items[*].type", containsInAnyOrder(
						"LOAN_CHECKED_OUT", "RESERVATION_PLACED", "LOAN_RETURNED", "RESERVATION_HELD",
						"RESERVATION_FULFILLED", "LOAN_CHECKED_OUT", "RESERVATION_PLACED", "RESERVATION_CANCELLED",
						"LOAN_CHECKED_OUT", "LOAN_RENEWED"
				)))
				.andExpect(jsonPath("$.items[0].actorUsername").value("reader-a"));

		mockMvc.perform(get("/api/activity")
						.with(user("reader-a").roles("PATRON"))
						.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(6))
				.andExpect(jsonPath("$.items[*].type", containsInAnyOrder(
						"LOAN_CHECKED_OUT", "LOAN_RETURNED", "RESERVATION_PLACED", "RESERVATION_CANCELLED",
						"LOAN_CHECKED_OUT", "LOAN_RENEWED"
				)));

		mockMvc.perform(get("/api/activity")
						.with(user("reader-b").roles("PATRON"))
						.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(4))
				.andExpect(jsonPath("$.items[*].type", containsInAnyOrder(
						"RESERVATION_PLACED", "RESERVATION_HELD", "RESERVATION_FULFILLED", "LOAN_CHECKED_OUT"
				)));
	}

	@Test
	void rejectsAnonymousAndInvalidActivityQueries() throws Exception {
		ensureStaff("staff-a");
		mockMvc.perform(get("/api/activity"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/activity")
						.with(user("staff-a").roles("STAFF"))
						.param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_page"));
		mockMvc.perform(get("/api/activity")
						.with(user("staff-a").roles("STAFF"))
						.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_page_size"));
		mockMvc.perform(get("/api/activity")
						.with(user("staff-a").roles("STAFF"))
						.param("type", "LOAN_DELETE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_activity_type"));
	}

	@Test
	void activityRowsRollBackWithTheOwningTransaction() {
		ensureStaff("staff-a");
		Member member = memberRepository.saveAndFlush(new Member("Rollback Reader", null, null, null));
		Book book = bookRepository.saveAndFlush(new Book("Rollback Book", "Rollback Author", null, null, null, null));
		Reservation reservation = new Reservation(member, book);

		new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus -> {
			activityService.recordReservationEvent(ActivityEventType.RESERVATION_PLACED, reservation, "staff-a");
			transactionStatus.setRollbackOnly();
		});

		assertEquals(0, activityEventRepository.count());
	}

	@Test
	void activitySnapshotsDoNotReferenceOrBlockDeletionOfCatalogAndMemberRows() throws Exception {
		ensureStaff("staff-a");
		Member member = memberRepository.saveAndFlush(new Member("Snapshot Reader", null, null, null));
		Book book = bookRepository.saveAndFlush(new Book("Snapshot Book", "Snapshot Author", null, null, null, null));
		Reservation reservation = new Reservation(member, book);
		activityService.recordReservationEvent(ActivityEventType.RESERVATION_PLACED, reservation, "staff-a");

		memberRepository.delete(member);
		bookRepository.delete(book);

		mockMvc.perform(get("/api/activity")
						.with(user("staff-a").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].memberName").value("Snapshot Reader"))
				.andExpect(jsonPath("$.items[0].bookTitle").value("Snapshot Book"))
				.andExpect(jsonPath("$.items[0].actorUsername").value("staff-a"));
	}

	private void ensureStaff(String username) {
		if (!staffAccountRepository.existsByUsername(username)) {
			staffAccountRepository.saveAndFlush(new StaffAccount(username, "test-password-hash"));
		}
	}

	private static long responseId(MvcResult result) throws Exception {
		String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();
		return Long.parseLong(id);
	}
}
