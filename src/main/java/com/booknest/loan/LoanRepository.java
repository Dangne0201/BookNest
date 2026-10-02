package com.booknest.loan;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

	boolean existsByActiveCopyId(Long copyId);

	boolean existsByBookCopyId(Long copyId);

	boolean existsByMemberId(Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select loan from Loan loan where loan.id = :loanId")
	Optional<Loan> findByIdForUpdate(@Param("loanId") Long loanId);

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy"
	})
	List<Loan> findAllByOrderByCheckoutDateDescIdDesc();

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy"
	})
	List<Loan> findAllByMemberAccountUsernameOrderByCheckoutDateDescIdDesc(String username);

	@EntityGraph(attributePaths = {
			"member",
			"member.account",
			"bookCopy",
			"bookCopy.book",
			"checkedOutBy",
			"returnedBy"
	})
	Optional<Loan> findWithDetailsById(Long id);
}
