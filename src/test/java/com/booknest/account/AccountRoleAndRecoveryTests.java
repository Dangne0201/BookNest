package com.booknest.account;

import com.booknest.loan.LoanRepository;
import com.booknest.member.MemberRepository;
import com.booknest.reservation.ReservationRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
@Transactional
class AccountRoleAndRecoveryTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private StaffAccountRepository accountRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private AdminIdentityRepository adminIdentityRepository;

	@Autowired
	private PasswordResetEventRepository passwordResetEventRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private SessionRegistry sessionRegistry;

	@BeforeEach
	void clearRecords() {
		loanRepository.deleteAll();
		reservationRepository.deleteAll();
		passwordResetEventRepository.deleteAll();
		adminIdentityRepository.deleteAll();
		memberRepository.deleteAll();
		accountRepository.deleteAll();
	}

	@Test
	void publicRegistrationCreatesPatronAndLinkedMember() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"username":"PatronOne","password":"reader-password",
								 "fullName":"  Reader One  ","email":"reader@example.test",
								 "phone":"  ","notes":"  "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("PATRON"))
				.andExpect(jsonPath("$.memberId").isNumber())
				.andExpect(jsonPath("$.passwordChangeRequired").value(false))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		StaffAccount account = accountRepository.findByUsername("patronone").orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(StaffAccount.Role.PATRON, account.getRole());
		org.junit.jupiter.api.Assertions.assertEquals("Reader One", account.getMember().getFullName());
		org.junit.jupiter.api.Assertions.assertEquals("reader@example.test", account.getMember().getEmail());
		org.junit.jupiter.api.Assertions.assertNull(account.getMember().getPhone());
		org.junit.jupiter.api.Assertions.assertNull(account.getMember().getNotes());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(
				"reader-password",
				account.getPasswordHash()
		));
	}

	@Test
	void patronCanReadAndUpdateOnlyTheirOwnProfile() throws Exception {
		accountService.registerPatron(new RegistrationRequest(
				"profile-reader", "reader-password", "Profile Reader", null, null, null
		));
		accountService.registerPatron(new RegistrationRequest(
				"another-reader", "reader-password", "Another Reader", null, null, null
		));

		mockMvc.perform(get("/api/members/me").with(user("profile-reader").roles("PATRON")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Profile Reader"));
		mockMvc.perform(put("/api/members/me")
						.with(user("profile-reader").roles("PATRON"))
						.with(csrf())
						.contentType(APPLICATION_JSON)
						.content("""
								{"fullName":"  Updated Reader  ","email":"updated@example.test",
								 "phone":"  ","notes":"  "}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Updated Reader"))
				.andExpect(jsonPath("$.phone").doesNotExist());

		org.junit.jupiter.api.Assertions.assertEquals(
				"Another Reader",
				accountRepository.findByUsername("another-reader").orElseThrow().getMember().getFullName()
		);
	}

	@Test
	void patronCannotReadAllMembersOrAdminAccountOperations() throws Exception {
		accountService.registerPatron(new RegistrationRequest(
				"reader", "reader-password", "Reader", null, null, null
		));

		mockMvc.perform(get("/api/members").with(user("reader").roles("PATRON")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/accounts").with(user("reader").roles("PATRON")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/accounts/staff")
						.with(user("reader").roles("PATRON"))
						.with(csrf())
						.contentType("application/json")
						.content("{\"username\":\"new-staff\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanIssueOneTimeResetCredentialAndTargetMustChangeIt() throws Exception {
		StaffAccount admin = accountRepository.save(new StaffAccount(
				"admin", passwordEncoder.encode("admin-password"), StaffAccount.Role.ADMIN, false
		));
		adminIdentityRepository.save(new AdminIdentity(admin));
		TemporaryPasswordResponse staff = accountService.createStaff("librarian", "admin");

		mockMvc.perform(post("/api/admin/accounts/{accountId}/password-reset", staff.accountId())
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("librarian"))
				.andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		StaffAccount resetAccount = accountRepository.findByUsername("librarian").orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(resetAccount.isPasswordChangeRequired());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(
				"admin-password",
				accountRepository.findByUsername("admin").orElseThrow().getPasswordHash()
		));
		org.junit.jupiter.api.Assertions.assertEquals(2, passwordResetEventRepository.count());

		mockMvc.perform(get("/api/admin/accounts").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.username == 'librarian')].passwordChangeRequired").value(true));
	}

	@Test
	void adminRecoveryCreatesOneAdminAndRotatesToTemporaryPassword() {
		TemporaryPasswordResponse first = accountService.recoverAdmin();
		StaffAccount account = accountRepository.findByUsername("admin").orElseThrow();
		TemporaryPasswordResponse second = accountService.recoverAdmin();

		org.junit.jupiter.api.Assertions.assertEquals(first.accountId(), second.accountId());
		org.junit.jupiter.api.Assertions.assertEquals("admin", second.username());
		org.junit.jupiter.api.Assertions.assertEquals(StaffAccount.Role.ADMIN, account.getRole());
		org.junit.jupiter.api.Assertions.assertTrue(account.isPasswordChangeRequired());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(
				second.temporaryPassword(),
				account.getPasswordHash()
		));
		org.junit.jupiter.api.Assertions.assertEquals(1, accountRepository.count());
		org.junit.jupiter.api.Assertions.assertEquals(1, adminIdentityRepository.count());
		org.junit.jupiter.api.Assertions.assertEquals(2, passwordResetEventRepository.count());
	}

	@Test
	void temporaryStaffPasswordAllowsOnlyPasswordChangeUntilItIsReplaced() throws Exception {
		createAdmin();
		TemporaryPasswordResponse temporary = accountService.createStaff("new-librarian", "admin");
		var csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession loginSession = (MockHttpSession) csrfResult.getRequest().getSession(false);
		String csrfJson = csrfResult.getResponse().getContentAsString();
		String csrfHeader = JsonPath.read(csrfJson, "$.headerName");
		String csrfToken = JsonPath.read(csrfJson, "$.token");

		var loginResult = mockMvc.perform(post("/api/auth/login")
						.session(loginSession)
						.header(csrfHeader, csrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", temporary.username())
						.param("password", temporary.temporaryPassword()))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
		mockMvc.perform(get("/api/members").session(authenticatedSession))
				.andExpect(status().isForbidden());

		var secondCsrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession secondLoginSession = (MockHttpSession) secondCsrfResult.getRequest().getSession(false);
		String secondCsrfJson = secondCsrfResult.getResponse().getContentAsString();
		String secondCsrfHeader = JsonPath.read(secondCsrfJson, "$.headerName");
		String secondCsrfToken = JsonPath.read(secondCsrfJson, "$.token");
		var secondLoginResult = mockMvc.perform(post("/api/auth/login")
						.session(secondLoginSession)
						.header(secondCsrfHeader, secondCsrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", temporary.username())
						.param("password", temporary.temporaryPassword()))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession secondAuthenticatedSession =
				(MockHttpSession) secondLoginResult.getRequest().getSession(false);

		var authenticatedCsrf = mockMvc.perform(get("/api/auth/csrf").session(authenticatedSession))
				.andExpect(status().isOk())
				.andReturn();
		String authenticatedCsrfJson = authenticatedCsrf.getResponse().getContentAsString();
		String currentCsrfHeader = JsonPath.read(authenticatedCsrfJson, "$.headerName");
		String currentCsrfToken = JsonPath.read(authenticatedCsrfJson, "$.token");
		mockMvc.perform(post("/api/auth/password")
						.session(authenticatedSession)
						.header(currentCsrfHeader, currentCsrfToken)
						.contentType(APPLICATION_JSON)
						.content("""
								{"currentPassword":"%s","newPassword":"permanent-library-password"}
								""".formatted(temporary.temporaryPassword())))
				.andExpect(status().isNoContent());
		org.junit.jupiter.api.Assertions.assertTrue(authenticatedSession.isInvalid());

		StaffAccount updatedAccount = accountRepository.findByUsername(temporary.username()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertFalse(updatedAccount.isPasswordChangeRequired());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(
				"permanent-library-password",
				updatedAccount.getPasswordHash()
		));
		mockMvc.perform(get("/api/auth/me").session(secondAuthenticatedSession))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("session_expired"));
	}

	@Test
	void adminResetExpiresAnExistingPatronSession() throws Exception {
		accountService.registerPatron(new RegistrationRequest(
				"session-reader", "reader-password", "Session Reader", null, null, null
		));
		createAdmin();

		var csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession loginSession = (MockHttpSession) csrfResult.getRequest().getSession(false);
		String csrfJson = csrfResult.getResponse().getContentAsString();
		String csrfHeader = JsonPath.read(csrfJson, "$.headerName");
		String csrfToken = JsonPath.read(csrfJson, "$.token");
		var loginResult = mockMvc.perform(post("/api/auth/login")
						.session(loginSession)
						.header(csrfHeader, csrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", "session-reader")
						.param("password", "reader-password"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession patronSession = (MockHttpSession) loginResult.getRequest().getSession(false);
		var registeredPatron = userDetailsService.loadUserByUsername("session-reader");
		org.junit.jupiter.api.Assertions.assertFalse(
				sessionRegistry.getAllSessions(registeredPatron, false).isEmpty()
		);

		long patronId = accountRepository.findByUsername("session-reader").orElseThrow().getId();
		mockMvc.perform(post("/api/admin/accounts/{accountId}/password-reset", patronId)
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().isOk());

		org.junit.jupiter.api.Assertions.assertTrue(sessionRegistry.getAllSessions(registeredPatron, true).stream()
				.anyMatch(org.springframework.security.core.session.SessionInformation::isExpired));
		mockMvc.perform(get("/api/auth/me").session(patronSession))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("session_expired"));
	}

	@Test
	void credentialRotationFromAnotherProcessInvalidatesTheExistingSession() throws Exception {
		accountService.registerPatron(new RegistrationRequest(
				"remote-reset-reader", "reader-password", "Remote Reset Reader", null, null, null
		));
		var csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession loginSession = (MockHttpSession) csrfResult.getRequest().getSession(false);
		String csrfJson = csrfResult.getResponse().getContentAsString();
		String csrfHeader = JsonPath.read(csrfJson, "$.headerName");
		String csrfToken = JsonPath.read(csrfJson, "$.token");
		var loginResult = mockMvc.perform(post("/api/auth/login")
						.session(loginSession)
						.header(csrfHeader, csrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", "remote-reset-reader")
						.param("password", "reader-password"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession patronSession = (MockHttpSession) loginResult.getRequest().getSession(false);

		StaffAccount account = accountRepository.findByUsername("remote-reset-reader").orElseThrow();
		account.resetPassword(passwordEncoder.encode("one-time-password"));
		accountRepository.saveAndFlush(account);

		mockMvc.perform(get("/api/auth/me").session(patronSession))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("session_expired"));
		org.junit.jupiter.api.Assertions.assertTrue(patronSession.isInvalid());
	}

	private StaffAccount createAdmin() {
		StaffAccount admin = accountRepository.save(new StaffAccount(
				"admin", passwordEncoder.encode("admin-password"), StaffAccount.Role.ADMIN, false
		));
		adminIdentityRepository.save(new AdminIdentity(admin));
		return admin;
	}
}
