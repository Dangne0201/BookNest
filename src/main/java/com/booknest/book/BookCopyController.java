package com.booknest.book;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{bookId}/copies")
public class BookCopyController {

	private final BookCopyService bookCopyService;

	public BookCopyController(BookCopyService bookCopyService) {
		this.bookCopyService = bookCopyService;
	}

	@GetMapping
	public List<BookCopyResponse> findAll(@PathVariable long bookId) {
		return bookCopyService.findAll(bookId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookCopyResponse create(
			@PathVariable long bookId,
			@Valid @RequestBody BookCopyRequest request
	) {
		return bookCopyService.create(bookId, request);
	}

	@PutMapping("/{copyId}")
	public BookCopyResponse update(
			@PathVariable long bookId,
			@PathVariable long copyId,
			@Valid @RequestBody BookCopyRequest request
	) {
		return bookCopyService.update(bookId, copyId, request);
	}

	@DeleteMapping("/{copyId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long bookId, @PathVariable long copyId) {
		bookCopyService.delete(bookId, copyId);
	}
}
