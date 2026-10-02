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
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
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
				.andExpect(jsonPath("$[0].id").value(loanId))
				.andExpect(jsonPath("$[0].memberName").value("Loan Test Member"))
				.andExpect(jsonPath("$[0].bookTitle").value("Loan Test Book"));

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
