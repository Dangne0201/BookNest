package com.booknest.book;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

	boolean existsByIsbn(String isbn);

	boolean existsByIsbnAndIdNot(String isbn, Long id);

	List<Book> findAllByOrderByTitleAscIdAsc();

	@Query(
			value = """
					select book from Book book
					where (:query = ''
						or lower(book.title) like lower(concat('%', :query, '%'))
						or lower(book.author) like lower(concat('%', :query, '%'))
						or lower(book.isbn) like lower(concat('%', :query, '%')))
					and (:genre = '' or lower(book.genre) = lower(:genre))
					and (:availability = ''
						or (:availability = 'AVAILABLE' and exists (
							select copy.id from BookCopy copy
							where copy.book = book and copy.status = com.booknest.book.BookCopy.Status.AVAILABLE
						))
						or (:availability = 'UNAVAILABLE' and not exists (
							select copy.id from BookCopy copy
							where copy.book = book and copy.status = com.booknest.book.BookCopy.Status.AVAILABLE
						)))
					""",
			countQuery = """
					select count(book) from Book book
					where (:query = ''
						or lower(book.title) like lower(concat('%', :query, '%'))
						or lower(book.author) like lower(concat('%', :query, '%'))
						or lower(book.isbn) like lower(concat('%', :query, '%')))
					and (:genre = '' or lower(book.genre) = lower(:genre))
					and (:availability = ''
						or (:availability = 'AVAILABLE' and exists (
							select copy.id from BookCopy copy
							where copy.book = book and copy.status = com.booknest.book.BookCopy.Status.AVAILABLE
						))
						or (:availability = 'UNAVAILABLE' and not exists (
							select copy.id from BookCopy copy
							where copy.book = book and copy.status = com.booknest.book.BookCopy.Status.AVAILABLE
						)))
					"""
	)
	Page<Book> search(
			@Param("query") String query,
			@Param("genre") String genre,
			@Param("availability") String availability,
			Pageable pageable
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select book from Book book where book.id = :bookId")
	Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);
}
