package com.booknest.reservation;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	boolean existsByActiveMemberBookKey(String activeMemberBookKey);

	long countByBookIdAndStatusAndIdLessThan(Long bookId, Reservation.Status status, Long id);

	@EntityGraph(attributePaths = {"member", "member.account", "book", "copy"})
	List<Reservation> findAllByOrderByIdDesc();

	@EntityGraph(attributePaths = {"member", "member.account", "book", "copy"})
	List<Reservation> findAllByMemberAccountUsernameOrderByIdDesc(String username);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select reservation from Reservation reservation
			where reservation.book.id = :bookId and reservation.status = :status
			order by reservation.id
			""")
	List<Reservation> findWaitingByBookForUpdate(
			@Param("bookId") Long bookId,
			@Param("status") Reservation.Status status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select reservation from Reservation reservation where reservation.id = :id")
	Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
