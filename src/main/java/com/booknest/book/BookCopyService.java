package com.booknest.book;

import java.util.List;

import com.booknest.loan.LoanRepository;
import com.booknest.reservation.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookCopyService {

	private final BookRepository bookRepository;
	private final BookCopyRepository bookCopyRepository;
	private final LoanRepository loanRepository;
	private final ReservationService reservationService;

	public BookCopyService(
			BookRepository bookRepository,
			BookCopyRepository bookCopyRepository,
			LoanRepository loanRepository,
			ReservationService reservationService
	) {
		this.bookRepository = bookRepository;
		this.bookCopyRepository = bookCopyRepository;
		this.loanRepository = loanRepository;
		this.reservationService = reservationService;
	}

	@Transactional(readOnly = true)
	public List<BookCopyResponse> findAll(long bookId) {
		requireBook(bookId);
		return bookCopyRepository.findAllByBookIdOrderByIdAsc(bookId).stream()
				.map(BookCopyService::toResponse)
				.toList();
	}

	@Transactional
	public BookCopyResponse create(long bookId, BookCopyRequest request) {
		Book book = bookRepository.findByIdForUpdate(bookId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
		if (request.status() == BookCopy.Status.ON_LOAN || request.status() == BookCopy.Status.ON_HOLD) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "copy_status_managed_by_loans");
		}
		BookCopy copy = bookCopyRepository.save(new BookCopy(book, request.status()));
		if (copy.getStatus() == BookCopy.Status.AVAILABLE) {
			reservationService.holdNext(bookId, copy);
		}
		return toResponse(copy);
	}

	@Transactional
	public BookCopyResponse update(long bookId, long copyId, BookCopyRequest request) {
		bookRepository.findByIdForUpdate(bookId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
		BookCopy copy = getCopyForUpdate(bookId, copyId);
		if (request.status() == BookCopy.Status.ON_LOAN || request.status() == BookCopy.Status.ON_HOLD) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "copy_status_managed_by_loans");
		}
		if (copy.getStatus() == BookCopy.Status.ON_HOLD) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_copy_reserved");
		}
		if (loanRepository.existsByActiveCopyId(copyId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_copy_on_loan");
		}
		copy.updateStatus(request.status());
		if (copy.getStatus() == BookCopy.Status.AVAILABLE) {
			reservationService.holdNext(bookId, copy);
		}
		return toResponse(bookCopyRepository.save(copy));
	}

	@Transactional
	public void delete(long bookId, long copyId) {
		bookRepository.findByIdForUpdate(bookId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
		BookCopy copy = getCopyForUpdate(bookId, copyId);
		if (copy.getStatus() == BookCopy.Status.ON_HOLD) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_copy_reserved");
		}
		if (loanRepository.existsByBookCopyId(copyId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "book_copy_has_loan_history");
		}
		bookCopyRepository.delete(copy);
	}

	private Book requireBook(long bookId) {
		return bookRepository.findById(bookId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_not_found"));
	}

	private BookCopy getCopyForUpdate(long bookId, long copyId) {
		requireBook(bookId);
		return bookCopyRepository.findByIdAndBookIdForUpdate(copyId, bookId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book_copy_not_found"));
	}

	private static BookCopyResponse toResponse(BookCopy copy) {
		return new BookCopyResponse(copy.getId(), copy.getBook().getId(), copy.getStatus());
	}
}
