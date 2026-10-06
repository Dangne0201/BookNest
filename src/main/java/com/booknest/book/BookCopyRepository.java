package com.booknest.book;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

	boolean existsByBookId(Long bookId);

	long countByBookId(Long bookId);

	long countByBookIdAndStatus(Long bookId, BookCopy.Status status);

	List<BookCopy> findAllByBookIdOrderByIdAsc(Long bookId);

	Optional<BookCopy> findByIdAndBookId(Long id, Long bookId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select copy from BookCopy copy where copy.id = :copyId")
	Optional<BookCopy> findByIdForUpdate(@Param("copyId") Long copyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select copy from BookCopy copy where copy.id = :copyId and copy.book.id = :bookId")
	Optional<BookCopy> findByIdAndBookIdForUpdate(
			@Param("copyId") Long copyId,
			@Param("bookId") Long bookId
	);

	@Query("""
			select copy.book.id as bookId,
			       count(copy.id) as totalCopies,
			       sum(case when copy.status = :availableStatus then 1 else 0 end) as availableCopies
			from BookCopy copy
			group by copy.book.id
			""")
	List<InventoryCount> findInventoryCounts(@Param("availableStatus") BookCopy.Status availableStatus);

	@Query("""
			select copy.book.id as bookId,
			       count(copy.id) as totalCopies,
			       sum(case when copy.status = :availableStatus then 1 else 0 end) as availableCopies
			from BookCopy copy
			where copy.book.id in :bookIds
			group by copy.book.id
			""")
	List<InventoryCount> findInventoryCountsByBookIdIn(
			@Param("bookIds") java.util.Collection<Long> bookIds,
			@Param("availableStatus") BookCopy.Status availableStatus
	);

	interface InventoryCount {
		Long getBookId();

		Long getTotalCopies();

		Long getAvailableCopies();
	}
}
