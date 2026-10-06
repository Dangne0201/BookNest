package com.booknest.loan;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

	boolean existsByActiveCopyId(Long copyId);

	boolean existsByBookCopyId(Long copyId);

	boolean existsByMemberId(Long memberId);

	long countByReturnDateIsNull();

	long countByReturnDateIsNullAndDueDateBefore(LocalDate date);

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy",
			"renewedBy"
	})
	@Query(
			value = """
					select loan from Loan loan
					where (:patronAccountId is null or loan.member.account.id = :patronAccountId)
					and (:query = ''
						or lower(loan.bookCopy.book.title) like lower(concat('%', :query, '%'))
						or lower(loan.member.fullName) like lower(concat('%', :query, '%')))
					and (:state is null
						or (:state = 'ACTIVE' and loan.returnDate is null and loan.dueDate >= :today)
						or (:state = 'OVERDUE' and loan.returnDate is null and loan.dueDate < :today)
						or (:state = 'RETURNED' and loan.returnDate is not null))
					""",
			countQuery = """
					select count(loan) from Loan loan
					where (:patronAccountId is null or loan.member.account.id = :patronAccountId)
					and (:query = ''
						or lower(loan.bookCopy.book.title) like lower(concat('%', :query, '%'))
						or lower(loan.member.fullName) like lower(concat('%', :query, '%')))
					and (:state is null
						or (:state = 'ACTIVE' and loan.returnDate is null and loan.dueDate >= :today)
						or (:state = 'OVERDUE' and loan.returnDate is null and loan.dueDate < :today)
						or (:state = 'RETURNED' and loan.returnDate is not null))
					"""
	)
	Page<Loan> search(
			@Param("patronAccountId") Long patronAccountId,
			@Param("query") String query,
			@Param("state") String state,
			@Param("today") LocalDate today,
			Pageable pageable
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select loan from Loan loan where loan.id = :loanId")
	Optional<Loan> findByIdForUpdate(@Param("loanId") Long loanId);

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy",
			"renewedBy"
	})
	List<Loan> findAllByOrderByCheckoutDateDescIdDesc();

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy",
			"renewedBy"
	})
	List<Loan> findAllByMemberAccountUsernameOrderByCheckoutDateDescIdDesc(String username);

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy",
			"renewedBy"
	})
	Optional<Loan> findWithDetailsById(Long id);
}
