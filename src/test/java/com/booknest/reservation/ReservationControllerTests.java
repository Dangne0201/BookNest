package com.booknest.reservation;

import java.time.LocalDate;

import com.booknest.account.AccountService;
import com.booknest.account.RegistrationRequest;
import com.booknest.account.StaffAccount;
import com.booknest.account.StaffAccountRepository;
import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.book.BookCopyRepository;
import com.booknest.book.BookRepository;
import com.booknest.loan.Loan;
import com.booknest.loan.LoanRepository;
import com.booknest.member.Member;
import com.booknest.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

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
class ReservationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

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

	@Test
	void patronsCheckoutImmediatelyUsingTheirOwnMemberProfile() throws Exception {
		var firstAccount = accountService.registerPatron(new RegistrationRequest(
				"reader-a", "reader-password", "Reader A", null, null, null
		));
		accountService.registerPatron(new RegistrationRequest(
				"reader-b", "reader-password", "Reader B", null, null, null
		));
		Book book = bookRepository.save(new Book("Shared title", "Author", null, null, null, null));
		BookCopy copy = copyRepository.save(new BookCopy(book, BookCopy.Status.AVAILABLE));

		mockMvc.perform(post("/api/loans")
						.with(user("reader-a").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberId":%d,"copyId":%d}
								""".formatted(memberRepository.findByAccountUsername("reader-b")
										.orElseThrow().getId(), copy.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId").value(firstAccount.memberId()))
				.andExpect(jsonPath("$.memberName").value("Reader A"));

		mockMvc.perform(get("/api/loans").with(user("reader-b").roles("PATRON")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(post("/api/loans/{loanId}/return", loanRepository.findAll().get(0).getId())
						.with(user("reader-a").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void fifoQueueHoldsReturnedCopyForNextPatronAndEnforcesOwnership() throws Exception {
		accountService.registerPatron(new RegistrationRequest(
				"reader-one", "reader-password", "Reader One", null, null, null
		));
		accountService.registerPatron(new RegistrationRequest(
				"reader-two", "reader-password", "Reader Two", null, null, null
		));
		Member borrower = memberRepository.save(new Member("Current Borrower", null, null, null));
		StaffAccount staff = accountRepository.save(new StaffAccount("librarian", "test-hash"));
		Book book = bookRepository.save(new Book("Queued title", "Author", null, null, null, null));
		BookCopy copy = copyRepository.save(new BookCopy(book, BookCopy.Status.ON_LOAN));
		Loan activeLoan = loanRepository.save(new Loan(
				borrower,
				copy,
				LocalDate.now().minusDays(1),
				LocalDate.now().plusDays(13),
				staff
		));

		var firstReservation = mockMvc.perform(post("/api/reservations")
						.with(user("reader-one").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bookId":%d}
								""".formatted(book.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("WAITING"))
				.andExpect(jsonPath("$.queuePosition").value(1))
				.andReturn();
		Number firstReservationValue = com.jayway.jsonpath.JsonPath.read(
				firstReservation.getResponse().getContentAsString(),
				"$.id"
		);
		long firstReservationId = firstReservationValue.longValue();

		mockMvc.perform(post("/api/reservations")
						.with(user("reader-two").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bookId":%d}
								""".formatted(book.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.queuePosition").value(2));

		mockMvc.perform(post("/api/loans/{loanId}/return", activeLoan.getId())
						.with(user("librarian").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isOk());
		org.junit.jupiter.api.Assertions.assertEquals(
				BookCopy.Status.ON_HOLD,
				copyRepository.findById(copy.getId()).orElseThrow().getStatus()
		);
		org.junit.jupiter.api.Assertions.assertEquals(Reservation.Status.HELD,
				reservationRepository.findById(firstReservationId).orElseThrow().getStatus());

		mockMvc.perform(get("/api/reservations").with(user("reader-two").roles("PATRON")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].memberName").value("Reader Two"))
				.andExpect(jsonPath("$[0].status").value("WAITING"))
				.andExpect(jsonPath("$[0].queuePosition").value(1));

		mockMvc.perform(post("/api/reservations/{reservationId}/checkout", firstReservationId)
						.with(user("reader-two").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/reservations/{reservationId}/checkout", firstReservationId)
						.with(user("reader-one").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberName").value("Reader One"));
		org.junit.jupiter.api.Assertions.assertEquals(
				BookCopy.Status.ON_LOAN,
				copyRepository.findById(copy.getId()).orElseThrow().getStatus()
		);
	}

	@Test
	void patronsCannotReserveAvailableBooksAndCanCancelTheirOwnWaitlistEntry() throws Exception {
		accountService.registerPatron(new RegistrationRequest(
				"reader-three", "reader-password", "Reader Three", null, null, null
		));
		Book availableBook = bookRepository.save(new Book("Available", "Author", null, null, null, null));
		copyRepository.save(new BookCopy(availableBook, BookCopy.Status.AVAILABLE));

		mockMvc.perform(post("/api/reservations")
						.with(user("reader-three").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bookId":%d}
								""".formatted(availableBook.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("book_available_for_checkout"));

		Book unavailableBook = bookRepository.save(new Book("Unavailable", "Author", null, null, null, null));
		copyRepository.save(new BookCopy(unavailableBook, BookCopy.Status.MAINTENANCE));
		var response = mockMvc.perform(post("/api/reservations")
						.with(user("reader-three").roles("PATRON"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bookId":%d}
								""".formatted(unavailableBook.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		Number reservationValue = com.jayway.jsonpath.JsonPath.read(
				response.getResponse().getContentAsString(),
				"$.id"
		);
		long reservationId = reservationValue.longValue();
		mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId)
						.with(user("reader-three").roles("PATRON"))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/reservations").with(user("reader-three").roles("PATRON")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("CANCELLED"));
	}
}
