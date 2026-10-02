package com.booknest.security;

import com.booknest.account.AccountService;
import com.booknest.account.RegistrationRequest;
import com.booknest.account.StaffAccountRepository;
import com.booknest.loan.LoanRepository;
import com.booknest.member.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
class SecurityConfigurationTests {

	private static final String PASSWORD = "secure-library-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private StaffAccountRepository staffAccountRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private LoanRepository loanRepository;

	@BeforeEach
	void clearAccounts() {
		loanRepository.deleteAll();
		memberRepository.deleteAll();
		staffAccountRepository.deleteAll();
	}

	@Test
	void anonymousRequestCannotReadCurrentAccount() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.error").value("unauthorized"));
	}

	@Test
	void registrationRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(APPLICATION_JSON)
						.content("""
								{"username":"new-staff","password":"secure-library-password"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void csrfBootstrapReturnsTheHeaderNameAndToken() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void loginCreatesAuthenticatedSessionAndLogoutInvalidatesIt() throws Exception {
		accountService.registerPatron(new RegistrationRequest("staff-login", PASSWORD));

		MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
		String csrfResponse = csrfResult.getResponse().getContentAsString();
		String csrfToken = JsonPath.read(csrfResponse, "$.token");
		String csrfHeaderName = JsonPath.read(csrfResponse, "$.headerName");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.session(session)
						.header(csrfHeaderName, csrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", "STAFF-LOGIN")
						.param("password", PASSWORD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true))
				.andReturn();

		MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("staff-login"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		MvcResult authenticatedCsrfResult = mockMvc.perform(get("/api/auth/csrf").session(authenticatedSession))
				.andExpect(status().isOk())
				.andReturn();
		String authenticatedCsrfResponse = authenticatedCsrfResult.getResponse().getContentAsString();
		String authenticatedCsrfHeaderName = JsonPath.read(authenticatedCsrfResponse, "$.headerName");
		String authenticatedCsrfToken = JsonPath.read(authenticatedCsrfResponse, "$.token");

		mockMvc.perform(post("/api/auth/logout")
						.session(authenticatedSession)
						.header(authenticatedCsrfHeaderName, authenticatedCsrfToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void changingPasswordInvalidatesCurrentSessionAndNewPasswordCanSignIn() throws Exception {
		accountService.registerPatron(new RegistrationRequest("staff-change-password", PASSWORD));

		MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
		String csrfResponse = csrfResult.getResponse().getContentAsString();
		String csrfToken = JsonPath.read(csrfResponse, "$.token");
		String csrfHeaderName = JsonPath.read(csrfResponse, "$.headerName");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.session(session)
						.header(csrfHeaderName, csrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", "staff-change-password")
						.param("password", PASSWORD))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);

		MvcResult authenticatedCsrfResult = mockMvc.perform(get("/api/auth/csrf").session(authenticatedSession))
				.andExpect(status().isOk())
				.andReturn();
		String authenticatedCsrfResponse = authenticatedCsrfResult.getResponse().getContentAsString();
		String authenticatedCsrfToken = JsonPath.read(authenticatedCsrfResponse, "$.token");
		String authenticatedCsrfHeader = JsonPath.read(authenticatedCsrfResponse, "$.headerName");
		String newPassword = "updated-secure-library-password";

		mockMvc.perform(post("/api/auth/password")
						.session(authenticatedSession)
						.header(authenticatedCsrfHeader, authenticatedCsrfToken)
						.contentType(APPLICATION_JSON)
						.content("""
								{"currentPassword":"%s","newPassword":"%s"}
								""".formatted(PASSWORD, newPassword)))
				.andExpect(status().isNoContent());

		org.junit.jupiter.api.Assertions.assertTrue(authenticatedSession.isInvalid());
		mockMvc.perform(get("/api/auth/me").session(new MockHttpSession()))
				.andExpect(status().isUnauthorized());

		MvcResult newCsrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession newSession = (MockHttpSession) newCsrfResult.getRequest().getSession(false);
		String newCsrfResponse = newCsrfResult.getResponse().getContentAsString();
		String newCsrfToken = JsonPath.read(newCsrfResponse, "$.token");
		String newCsrfHeader = JsonPath.read(newCsrfResponse, "$.headerName");

		mockMvc.perform(post("/api/auth/login")
						.session(newSession)
						.header(newCsrfHeader, newCsrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", "staff-change-password")
						.param("password", newPassword))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true));

		MvcResult oldPasswordCsrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		String oldPasswordCsrfResponse = oldPasswordCsrfResult.getResponse().getContentAsString();
		String oldPasswordCsrfHeader = JsonPath.read(oldPasswordCsrfResponse, "$.headerName");
		String oldPasswordCsrfToken = JsonPath.read(oldPasswordCsrfResponse, "$.token");
		mockMvc.perform(post("/api/auth/login")
						.session((MockHttpSession) oldPasswordCsrfResult.getRequest().getSession(false))
						.header(oldPasswordCsrfHeader, oldPasswordCsrfToken)
						.contentType(APPLICATION_FORM_URLENCODED)
						.param("username", "staff-change-password")
						.param("password", PASSWORD))
				.andExpect(status().isUnauthorized());
	}
}
