package com.booknest.book;

public record BookResponse(
		Long id,
		String title,
		String author,
		String isbn,
		String genre,
		Integer publicationYear,
		String description,
		long totalCopies,
		long availableCopies
) {
}
