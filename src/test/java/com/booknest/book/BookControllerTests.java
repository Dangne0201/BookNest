package com.booknest.book;

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
class BookControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@BeforeEach
	void clearDatabase() {
		bookCopyRepository.deleteAll();
		bookRepository.deleteAll();
	}

	@Test
	void staffCanCreateReadUpdateAndDeleteBookTitles() throws Exception {
		String request = """
				{"title":"  Clean Code ","author":"Robert Martin","isbn":"978-0-306-40615-7",
				 "genre":"Software","publicationYear":2008,"description":"A guide"}
				""";

		mockMvc.perform(post("/api/books")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Clean Code"))
				.andExpect(jsonPath("$.isbn").value("9780306406157"))
				.andExpect(jsonPath("$.totalCopies").value(0))
				.andExpect(jsonPath("$.availableCopies").value(0));

		long bookId = bookRepository.findAll().get(0).getId();
		mockMvc.perform(get("/api/books").with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Clean Code"));
		mockMvc.perform(get("/api/books/{bookId}", bookId).with(user("staff-b").roles("STAFF")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.author").value("Robert Martin"));

		mockMvc.perform(put("/api/books/{bookId}", bookId)
						.with(user("staff-b").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"title":"Clean Code: Revised","author":"Robert Martin","isbn":null,
								 "genre":null,"publicationYear":2009,"description":null}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Clean Code: Revised"));

		mockMvc.perform(delete("/api/books/{bookId}", bookId)
						.with(user("staff-a").roles("STAFF"))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/books/{bookId}", bookId).with(user("staff-a").roles("STAFF")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("book_not_found"));
	}

	@Test
	void rejectsInvalidBookDataAndDuplicateNormalizedIsbn() throws Exception {
		mockMvc.perform(post("/api/books")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"title":" ","author":"","isbn":null,"genre":null,"publicationYear":null,"description":null}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_failed"));

		mockMvc.perform(post("/api/books")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"title":"First","author":"Author","isbn":"not-an-isbn"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_isbn_format"));

		mockMvc.perform(post("/api/books")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"title":"Future book","author":"Author","publicationYear":2200}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_publication_year"));

		mockMvc.perform(post("/api/books")
						.with(user("staff-a").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"title":"First","author":"Author","isbn":"978-0-306-40615-7"}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/books")
						.with(user("staff-b").roles("STAFF"))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{"title":"Second","author":"Author","isbn":"9780306406157"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("isbn_taken"));
	}

	@Test
	void bookApiRequiresAuthenticationAndCsrfForWrites() throws Exception {
		mockMvc.perform(get("/api/books"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/books")
						.with(user("staff-a").roles("STAFF"))
						.contentType("application/json")
						.content("""
								{"title":"Title","author":"Author"}
								"""))
				.andExpect(status().isForbidden());
	}
}
