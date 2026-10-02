package com.booknest.member;

import com.booknest.loan.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/cleanup.sql")
class MemberControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private LoanRepository loanRepository;

	@BeforeEach
	void clearMembers() {
		loanRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	void staffShareMemberRecordsAndCanCreateReadUpdateAndDelete() throws Exception {
		mockMvc.perform(post("/api/members")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"  Lan Nguyen  ","email":"lan@example.com",
								 "phone":"  +84 123 456  ","notes":"  Reader  "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName").value("Lan Nguyen"))
				.andExpect(jsonPath("$.email").value("lan@example.com"))
				.andExpect(jsonPath("$.phone").value("+84 123 456"))
				.andExpect(jsonPath("$.notes").value("Reader"));
		long memberId = memberRepository.findAll().get(0).getId();

		mockMvc.perform(get("/api/members").with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(memberId))
				.andExpect(jsonPath("$[0].fullName").value("Lan Nguyen"));

		mockMvc.perform(get("/api/members/{memberId}", memberId).with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("lan@example.com"));

		mockMvc.perform(put("/api/members/{memberId}", memberId)
						.with(user("staff-b").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Lan N.","email":null,"phone":"","notes":"Updated"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Lan N."))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.phone").doesNotExist())
				.andExpect(jsonPath("$.notes").value("Updated"));

		mockMvc.perform(delete("/api/members/{memberId}", memberId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/members/{memberId}", memberId).with(user("staff-a").roles("STAFF")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("member_not_found"));
	}

	@Test
	void rejectsInvalidMemberDataAndNormalizesBlankOptionalFields() throws Exception {
		mockMvc.perform(post("/api/members")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":" ","email":null,"phone":null,"notes":null}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));

		mockMvc.perform(post("/api/members")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"Lan Nguyen\",\"phone\":\"" + "x".repeat(31) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));

		mockMvc.perform(post("/api/members")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Lan Nguyen","email":"not-an-email"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));

		mockMvc.perform(post("/api/members")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Lan Nguyen","email":"  ","phone":" ","notes":" "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.phone").doesNotExist())
				.andExpect(jsonPath("$.notes").doesNotExist());
	}

	@Test
	void missingMembersReturnSafeNotFoundResponses() throws Exception {
		mockMvc.perform(get("/api/members/999999").with(user("staff-a").roles("STAFF")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("member_not_found"));

		mockMvc.perform(put("/api/members/999999", 999999)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Lan Nguyen"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("member_not_found"));

		mockMvc.perform(delete("/api/members/999999").with(user("staff-a").roles("STAFF")).with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("member_not_found"));
	}

	@Test
	void memberApiRequiresAuthenticationAndCsrfForWrites() throws Exception {
		mockMvc.perform(get("/api/members"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/members")
						.with(user("staff-a").roles("STAFF"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Lan Nguyen"}
								"""))
				.andExpect(status().isForbidden());
	}
}
