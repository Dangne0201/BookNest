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
@RequestMapping("/api/books")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@GetMapping
	public List<BookResponse> findAll() {
		return bookService.findAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookResponse create(@Valid @RequestBody BookRequest request) {
		return bookService.create(request);
	}

	@GetMapping("/{bookId}")
	public BookResponse findById(@PathVariable long bookId) {
		return bookService.findById(bookId);
	}

	@PutMapping("/{bookId}")
	public BookResponse update(@PathVariable long bookId, @Valid @RequestBody BookRequest request) {
		return bookService.update(bookId, request);
	}

	@DeleteMapping("/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long bookId) {
		bookService.delete(bookId);
	}
}
