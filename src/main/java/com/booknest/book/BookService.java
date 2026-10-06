package com.booknest.book;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.booknest.common.PageRequestFactory;
import com.booknest.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

	private static final Pattern ISBN_PATTERN = Pattern.compile("(?:\\d{9}[\\dX]|\\d{13})");

	private final BookRepository bookRepository;
	private final BookCopyRepository bookCopyRepository;

	public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository) {
		this.bookRepository = bookRepository;
		this.bookCopyRepository = bookCopyRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<BookResponse> findAll(
			String query,
			String genre,
			String availability,
			int page,
			int size,
			String sort,
			String direction
	) {
		String normalizedAvailability = trimToNull(availability);
		if (normalizedAvailability != null) {
			normalizedAvailability = normalizedAvailability.toUpperCase(Locale.ROOT);
			if (!normalizedAvailability.equals("AVAILABLE") && !normalizedAvailability.equals("UNAVAILABLE")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_availability_filter");
			}
		}
		PageRequest pageable = PageRequestFactory.create(
				page,
				size,
				sort,
				direction,
				Map.of(
						"title", "title",
						"author", "author",
						"genre", "genre",
						"publicationYear", "publicationYear",
						"id", "id"
				),
				"title",
				"asc"
		);
		Page<Book> bookPage = bookRepository.search(
				query == null ? "" : query.trim(),
				genre == null ? "" : genre.trim(),
				normalizedAvailability == null ? "" : normalizedAvailability,
				pageable
		);
		List<Long> bookIds = bookPage.getContent().stream().map(Book::getId).toList();
		Map<Long, BookCopyRepository.InventoryCount> counts = new HashMap<>();
		if (!bookIds.isEmpty()) {
			bookCopyRepository.findInventoryCountsByBookIdIn(bookIds, BookCopy.Status.AVAILABLE)
					.forEach(count -> counts.put(count.getBookId(), count));
		}

		return PageResponse.from(bookPage.map(book -> toResponse(book, counts.get(book.getId()))));
	}

	@Transactional(readOnly = true)
	public List<BookResponse> findOptions() {
		Map<Long, BookCopyRepository.InventoryCount> counts = new HashMap<>();
		bookCopyRepository.findInventoryCounts(BookCopy.Status.AVAILABLE)
				.forEach(count -> counts.put(count.getBookId(), count));
		return bookRepository.findAllByOrderByTitleAscIdAsc().stream()
				.map(book -> toResponse(book, counts.get(book.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public BookResponse findById(long id) {
		Book book = getBook(id);
		return toResponseWithCounts(book, id);
	}

	@Transactional
	public BookResponse create(BookRequest request) {
		Book book = new Book(
				request.title().trim(),
				request.author().trim(),
				normalizeIsbn(request.isbn()),
				trimToNull(request.genre()),
				validatePublicationYear(request.publicationYear()),
				trimToNull(request.description())
		);
		checkIsbnAvailable(book.getIsbn(), null);
		return toResponse(bookRepository.save(book), null);
	}

	@Transactional
	public BookResponse update(long id, BookRequest request) {
		Book book = getBook(id);
		String isbn = normalizeIsbn(request.isbn());
		checkIsbnAvailable(isbn, id);
		book.updateDetails(
				request.title().trim(),
				request.author().trim(),
				isbn,
				trimToNull(request.genre()),
				validatePublicationYear(request.publicationYear()),
				trimToNull(request.description())
		);
		return toResponseWithCounts(bookRepository.save(book), id);
	}

	@Transactional
	public void delete(long id) {
		Book book = getBook(id);
		if (bookCopyRepository.existsByBookId(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_has_copies");
		}
		bookRepository.delete(book);
	}

	private Book getBook(long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
	}

	private void checkIsbnAvailable(String isbn, Long currentBookId) {
		if (isbn != null && (currentBookId == null
				? bookRepository.existsByIsbn(isbn)
				: bookRepository.existsByIsbnAndIdNot(isbn, currentBookId))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "isbn_taken");
		}
	}

	private BookResponse toResponseWithCounts(Book book, long bookId) {
		return toResponse(
				book,
				bookCopyRepository.countByBookId(bookId),
				bookCopyRepository.countByBookIdAndStatus(bookId, BookCopy.Status.AVAILABLE)
		);
	}

	private static BookResponse toResponse(Book book, long totalCopies, long availableCopies) {
		return new BookResponse(
				book.getId(),
				book.getTitle(),
				book.getAuthor(),
				book.getIsbn(),
				book.getGenre(),
				book.getPublicationYear(),
				book.getDescription(),
				totalCopies,
				availableCopies
		);
	}

	private static BookResponse toResponse(Book book, BookCopyRepository.InventoryCount count) {
		long totalCopies = count == null ? 0 : count.getTotalCopies();
		long availableCopies = count == null ? 0 : count.getAvailableCopies();
		return new BookResponse(
				book.getId(),
				book.getTitle(),
				book.getAuthor(),
				book.getIsbn(),
				book.getGenre(),
				book.getPublicationYear(),
				book.getDescription(),
				totalCopies,
				availableCopies
		);
	}

	private static String normalizeIsbn(String value) {
		String isbn = trimToNull(value);
		if (isbn == null) {
			return null;
		}
		isbn = isbn.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
		if (!ISBN_PATTERN.matcher(isbn).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_isbn_format");
		}
		return isbn;
	}

	private static Integer validatePublicationYear(Integer value) {
		if (value != null && (value < 1000 || value > Year.now().getValue() + 1)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_publication_year");
		}
		return value;
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
