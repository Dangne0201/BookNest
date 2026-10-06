package com.booknest.dashboard;

import java.time.LocalDate;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
class DashboardControllerTests {

	@Autowired
	private MockMvc mockMvc;

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

	@Test
	void returnsZeroCountsForAnEmptyLibrary() throws Exception {
		mockMvc.perform(get("/api/dashboard")
						.with(SecurityMockMvcRequestPostProcessors.user("staff").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCopies").value(0))
				.andExpect(jsonPath("$.activeLoans").value(0))
				.andExpect(jsonPath("$.overdueLoans").value(0))
				.andExpect(jsonPath("$.activeReservations").value(0));
	}

	@Test
	void countsSharedInventoryLoansAndOnlyActiveReservations() throws Exception {
		StaffAccount staff = staffAccountRepository.saveAndFlush(new StaffAccount("dashboard-staff", "test-hash"));
		StaffAccount admin = staffAccountRepository.saveAndFlush(new StaffAccount(
				"dashboard-admin", "test-hash", Role.ADMIN, false
		));
		Member firstMember = memberRepository.saveAndFlush(new Member("Dashboard Reader One", null, null, null));
		Member secondMember = memberRepository.saveAndFlush(new Member("Dashboard Reader Two", null, null, null));
		Book firstBook = bookRepository.saveAndFlush(new Book("Dashboard Book One", "Author", null, null, null, null));
		Book secondBook = bookRepository.saveAndFlush(new Book("Dashboard Book Two", "Author", null, null, null, null));
		Book thirdBook = bookRepository.saveAndFlush(new Book("Dashboard Book Three", "Author", null, null, null, null));

		bookCopyRepository.saveAndFlush(new BookCopy(firstBook, BookCopy.Status.AVAILABLE));
		bookCopyRepository.saveAndFlush(new BookCopy(firstBook, BookCopy.Status.AVAILABLE));
		BookCopy futureLoanCopy = bookCopyRepository.saveAndFlush(
				new BookCopy(firstBook, BookCopy.Status.ON_LOAN)
		);
		BookCopy overdueLoanCopy = bookCopyRepository.saveAndFlush(
				new BookCopy(secondBook, BookCopy.Status.ON_LOAN)
		);
		BookCopy returnedCopy = bookCopyRepository.saveAndFlush(
				new BookCopy(secondBook, BookCopy.Status.AVAILABLE)
		);
		BookCopy heldCopy = bookCopyRepository.saveAndFlush(new BookCopy(thirdBook, BookCopy.Status.ON_HOLD));
		bookCopyRepository.saveAndFlush(new BookCopy(thirdBook, BookCopy.Status.MAINTENANCE));
		bookCopyRepository.saveAndFlush(new BookCopy(thirdBook, BookCopy.Status.RETIRED));

		LocalDate today = LocalDate.now();
		loanRepository.saveAndFlush(new Loan(firstMember, futureLoanCopy, today, today.plusDays(7), staff));
		Loan overdueLoan = loanRepository.saveAndFlush(
				new Loan(firstMember, overdueLoanCopy, today.minusDays(20), today.minusDays(6), staff)
		);
		Loan returnedLoan = new Loan(secondMember, returnedCopy, today.minusDays(20), today.minusDays(6), staff);
		returnedLoan.returnOn(today.minusDays(2), staff);
		loanRepository.saveAndFlush(returnedLoan);

		Reservation waiting = new Reservation(firstMember, firstBook);
		reservationRepository.saveAndFlush(waiting);
		Reservation held = new Reservation(secondMember, thirdBook);
		held.hold(heldCopy);
		reservationRepository.saveAndFlush(held);
		Reservation cancelled = new Reservation(firstMember, secondBook);
		cancelled.cancel();
		reservationRepository.saveAndFlush(cancelled);
		Reservation fulfilled = new Reservation(secondMember, firstBook);
		fulfilled.fulfill();
		reservationRepository.saveAndFlush(fulfilled);

		assertSummary("dashboard-staff", "STAFF", 3, 2, 1, 2);
		assertSummary("dashboard-admin", "ADMIN", 3, 2, 1, 2);

		overdueLoan.returnOn(today, staff);
		loanRepository.saveAndFlush(overdueLoan);
		overdueLoanCopy.updateStatus(BookCopy.Status.AVAILABLE);
		bookCopyRepository.saveAndFlush(overdueLoanCopy);
		waiting.cancel();
		reservationRepository.saveAndFlush(waiting);

		assertSummary("dashboard-staff", "STAFF", 4, 1, 0, 1);
	}

	@Test
	void deniesDashboardAccessToPatronsAndAnonymousCallers() throws Exception {
		mockMvc.perform(get("/api/dashboard"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/dashboard")
						.with(SecurityMockMvcRequestPostProcessors.user("reader").roles("PATRON")))
				.andExpect(status().isForbidden());
	}

	private void assertSummary(
			String username,
			String role,
			int availableCopies,
			int activeLoans,
			int overdueLoans,
			int activeReservations
	) throws Exception {
		mockMvc.perform(get("/api/dashboard")
						.with(SecurityMockMvcRequestPostProcessors.user(username).roles(role)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCopies").value(availableCopies))
				.andExpect(jsonPath("$.activeLoans").value(activeLoans))
				.andExpect(jsonPath("$.overdueLoans").value(overdueLoans))
				.andExpect(jsonPath("$.activeReservations").value(activeReservations));
	}
}
