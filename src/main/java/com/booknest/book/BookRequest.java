package com.booknest.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 150) String author,
		@Size(max = 24) String isbn,
		@Size(max = 100) String genre,
		Integer publicationYear,
		@Size(max = 2000) String description
) {
}
