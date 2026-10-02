package com.booknest.book;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

	boolean existsByIsbn(String isbn);

	boolean existsByIsbnAndIdNot(String isbn, Long id);

	List<Book> findAllByOrderByTitleAscIdAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select book from Book book where book.id = :bookId")
	java.util.Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);
}
