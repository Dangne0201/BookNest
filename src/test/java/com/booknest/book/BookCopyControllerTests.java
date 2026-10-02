package com.booknest.book;

import com.booknest.loan.LoanRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

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
class BookCopyControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@Autowired
	private LoanRepository loanRepository;

	private long bookId;
	private long otherBookId;

	@BeforeEach
	void prepareBooks() {
		loanRepository.deleteAll();
		bookCopyRepository.deleteAll();
		bookRepository.deleteAll();
		bookId = bookRepository.save(new Book("Shared title", "Author", null, null, null, null)).getId();
		otherBookId = bookRepository.save(new Book("Other title", "Author", null, null, null, null)).getId();
	}

	@Test
	void staffShareCopyChangesAndAvailabilityAndCannotDeleteTitleWithCopies() throws Exception {
		String availableCopy = """
				{"status":"AVAILABLE"}
				""";
		long copyId = mockMvc.perform(post("/api/books/{bookId}/copies", bookId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content(availableCopy))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("AVAILABLE"))
				.andReturn()
				.getResponse()
				.getContentAsString()
				.lines()
				.findFirst()
				.map(body -> JsonPath.read(body, "$.id").toString())
				.map(Long::parseLong)
				.orElseThrow();

		mockMvc.perform(get("/api/books/{bookId}/copies", bookId).with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(copyId))
				.andExpect(jsonPath("$[0].status").value("AVAILABLE"));

		mockMvc.perform(get("/api/books/{bookId}", bookId).with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCopies").value(1))
				.andExpect(jsonPath("$.availableCopies").value(1));

		mockMvc.perform(put("/api/books/{bookId}/copies/{copyId}", bookId, copyId)
						.with(user("staff-b").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"status":"MAINTENANCE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("MAINTENANCE"));

		mockMvc.perform(get("/api/books/{bookId}", bookId).with(user("staff-a").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCopies").value(1))
				.andExpect(jsonPath("$.availableCopies").value(0));

		mockMvc.perform(delete("/api/books/{bookId}", bookId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("book_has_copies"));

		mockMvc.perform(delete("/api/books/{bookId}/copies/{copyId}", bookId, copyId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/books/{bookId}", bookId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void rejectsMissingBooksCopiesAndCopiesUnderTheWrongTitle() throws Exception {
		long copyId = bookCopyRepository.save(
				new BookCopy(bookRepository.findById(bookId).orElseThrow(), BookCopy.Status.AVAILABLE)
		).getId();

		mockMvc.perform(get("/api/books/999999/copies").with(user("staff-a").roles("STAFF")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("book_not_found"));

		mockMvc.perform(put("/api/books/{bookId}/copies/{copyId}", otherBookId, copyId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"status":"RETIRED"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("book_copy_not_found"));

		mockMvc.perform(post("/api/books/999999/copies")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"status":"AVAILABLE"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsInvalidCopyStatuses() throws Exception {
		mockMvc.perform(post("/api/books/{bookId}/copies", bookId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"status":"LOANED"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_request"));

		mockMvc.perform(post("/api/books/{bookId}/copies", bookId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"status":"ON_LOAN"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("copy_status_managed_by_loans"));
	}
}
