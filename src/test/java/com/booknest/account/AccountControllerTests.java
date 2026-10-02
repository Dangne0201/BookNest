package com.booknest.account;

import com.booknest.loan.LoanRepository;
import com.booknest.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
class AccountControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StaffAccountRepository staffAccountRepository;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserDetailsService userDetailsService;

	@BeforeEach
	void clearAccounts() {
		loanRepository.deleteAll();
		memberRepository.deleteAll();
		staffAccountRepository.deleteAll();
	}

	@Test
	void registrationStoresBcryptHashAndDoesNotReturnPassword() throws Exception {
		String password = "correct-horse-battery";

		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"username":"LibrarianOne","password":"%s","fullName":"Reader One"}
								""".formatted(password)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("librarianone"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		StaffAccount account = staffAccountRepository.findByUsername("librarianone").orElseThrow();
		org.junit.jupiter.api.Assertions.assertNotEquals(password, account.getPasswordHash());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(password, account.getPasswordHash()));
	}

	@Test
	void registrationRejectsDuplicateUsernameRegardlessOfCase() throws Exception {
		staffAccountRepository.save(new StaffAccount("reader", passwordEncoder.encode("valid-password")));

		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"username":"READER","password":"another-valid-password","fullName":"Another Reader"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("username_taken"))
				.andExpect(jsonPath("$.message").value("That username is already in use."));
	}

	@Test
	void registrationRejectsInvalidInput() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"username":" ","password":"short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));
	}

	@Test
	void authenticatedStaffCanChangeOnlyTheirPasswordAndReceivesNoPasswordData() throws Exception {
		String currentPassword = "current-password";
		String newPassword = "new-password-value";
		staffAccountRepository.save(new StaffAccount("password-owner", passwordEncoder.encode(currentPassword)));
		String otherPassword = "another-current-password";
		staffAccountRepository.save(new StaffAccount("another-staff", passwordEncoder.encode(otherPassword)));
		String otherPasswordHash = staffAccountRepository.findByUsername("another-staff").orElseThrow()
				.getPasswordHash();

		mockMvc.perform(post("/api/auth/password")
						.with(user("password-owner").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"currentPassword":"%s","newPassword":"%s"}
								""".formatted(currentPassword, newPassword)))
				.andExpect(status().isNoContent())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		StaffAccount account = staffAccountRepository.findByUsername("password-owner").orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(newPassword, account.getPasswordHash()));
		org.junit.jupiter.api.Assertions.assertFalse(passwordEncoder.matches(currentPassword, account.getPasswordHash()));
		org.junit.jupiter.api.Assertions.assertEquals(otherPasswordHash,
				staffAccountRepository.findByUsername("another-staff").orElseThrow().getPasswordHash());
	}

	@Test
	void passwordChangeRejectsWrongCurrentPasswordAndNewPasswordReuse() throws Exception {
		String currentPassword = "current-password";
		String newPassword = "different-password";
		staffAccountRepository.save(new StaffAccount("password-owner", passwordEncoder.encode(currentPassword)));

		mockMvc.perform(post("/api/auth/password")
						.with(user("password-owner").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"currentPassword":"not-the-current-password","newPassword":"%s"}
								""".formatted(newPassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_current_password"));
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(currentPassword,
				staffAccountRepository.findByUsername("password-owner").orElseThrow().getPasswordHash()));

		mockMvc.perform(post("/api/auth/password")
						.with(user("password-owner").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"currentPassword":"%s","newPassword":"%s"}
								""".formatted(currentPassword, currentPassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("password_unchanged"));
	}

	@Test
	void passwordChangeRequiresAuthenticationCsrfAndValidNewPassword() throws Exception {
		String password = "current-password";
		staffAccountRepository.save(new StaffAccount("password-owner", passwordEncoder.encode(password)));

		mockMvc.perform(post("/api/auth/password")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"currentPassword":"%s","newPassword":"new-password-value"}
								""".formatted(password)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/auth/password")
						.with(user("password-owner").roles("STAFF"))
						.contentType("application/json")
						.content("""
								{"currentPassword":"%s","newPassword":"new-password-value"}
								""".formatted(password)))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/auth/password")
						.with(user("password-owner").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"currentPassword":"%s","newPassword":"short"}
								""".formatted(password)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));

		mockMvc.perform(post("/api/auth/password")
						.with(user("password-owner").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"currentPassword":"%s","newPassword":"%s"}
								""".formatted(password, "x".repeat(73))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));
	}

	@Test
	void everyStaffAccountReceivesTheSameAuthority() {
		staffAccountRepository.save(new StaffAccount("staff-a", passwordEncoder.encode("password-one")));
		staffAccountRepository.save(new StaffAccount("staff-b", passwordEncoder.encode("password-two")));

		var first = userDetailsService.loadUserByUsername("staff-a");
		var second = userDetailsService.loadUserByUsername("staff-b");

		org.junit.jupiter.api.Assertions.assertEquals(first.getAuthorities(), second.getAuthorities());
		org.junit.jupiter.api.Assertions.assertEquals(1, first.getAuthorities().size());
		org.junit.jupiter.api.Assertions.assertEquals(
				"ROLE_STAFF",
				first.getAuthorities().iterator().next().getAuthority()
		);
	}
}
