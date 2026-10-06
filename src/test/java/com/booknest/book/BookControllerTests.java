package com.booknest.book;

import java.util.Set;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
				.andExpect(jsonPath("$.items[0].title").value("Clean Code"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.page").value(0));
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
	void searchesFiltersSortsAndPaginatesBooksInTheDatabase() throws Exception {
		Book available = bookRepository.save(new Book(
				"Alpha Reader", "Case Author", "9780132350884", "Fantasy", 2020, null
		));
		bookRepository.save(new Book(
				"Beta Reader", "Other Author", "9780135957059", "Fantasy", 2021, null
		));
		bookCopyRepository.save(new BookCopy(available, BookCopy.Status.AVAILABLE));
		Book sameTitleFirst = bookRepository.save(new Book("Same title", "Second author", null, null, null, null));
		Book sameTitleSecond = bookRepository.save(new Book("Same title", "First author", null, null, null, null));

		mockMvc.perform(get("/api/books"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(4));

		mockMvc.perform(get("/api/books")
						.param("q", "ALPHA")
						.param("genre", "fantasy")
						.param("availability", "AVAILABLE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(available.getId()))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/books")
						.param("availability", "UNAVAILABLE")
						.param("sort", "author")
						.param("direction", "desc")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].availableCopies").value(0))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(3));

		String sameTitlePage = getBookPageId(0);
		assertEquals(sameTitlePage, getBookPageId(0));
		String nextSameTitlePage = getBookPageId(1);
		assertNotEquals(sameTitlePage, nextSameTitlePage);
		assertTrue(Set.of(sameTitleFirst.getId().toString(), sameTitleSecond.getId().toString())
				.containsAll(Set.of(sameTitlePage, nextSameTitlePage)));
	}

	private String getBookPageId(int page) throws Exception {
		return mockMvc.perform(get("/api/books")
						.param("q", "same title")
						.param("sort", "title")
						.param("direction", "asc")
						.param("size", "1")
						.param("page", String.valueOf(page)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(page))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andReturn()
				.getResponse()
				.getContentAsString()
				.lines()
				.findFirst()
				.map(response -> JsonPath.read(response, "$.items[0].id").toString())
				.orElseThrow();
	}

	@Test
	void rejectsInvalidBookPagingSortingAndFilters() throws Exception {
		for (String query : new String[]{
				"?page=-1",
				"?size=0",
				"?size=101",
				"?sort=title%20desc",
				"?direction=sideways",
				"?availability=unknown"
		}) {
			mockMvc.perform(get("/api/books" + query))
					.andExpect(status().isBadRequest());
		}
		mockMvc.perform(get("/api/books").param("page", "not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_query_parameter"));
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
