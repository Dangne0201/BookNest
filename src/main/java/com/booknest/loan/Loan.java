package com.booknest.loan;

import java.time.LocalDate;
import java.time.Instant;

import com.booknest.account.StaffAccount;
import com.booknest.book.BookCopy;
import com.booknest.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "loans",
		uniqueConstraints = @UniqueConstraint(name = "uk_loans_active_copy", columnNames = "active_copy_id")
)
public class Loan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_copy_id", nullable = false)
	private BookCopy bookCopy;

	@Column(name = "active_copy_id")
	private Long activeCopyId;

	@Column(name = "checkout_date", nullable = false)
	private LocalDate checkoutDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "return_date")
	private LocalDate returnDate;

	@Column(name = "renewed_at")
	private Instant renewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "renewed_by_id")
	private StaffAccount renewedBy;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checkout_staff_id", nullable = false)
	private StaffAccount checkedOutBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "return_staff_id")
	private StaffAccount returnedBy;

	protected Loan() {
	}

	public Loan(
			Member member,
			BookCopy bookCopy,
			LocalDate checkoutDate,
			LocalDate dueDate,
			StaffAccount checkedOutBy
	) {
		this.member = member;
		this.bookCopy = bookCopy;
		this.activeCopyId = bookCopy.getId();
		this.checkoutDate = checkoutDate;
		this.dueDate = dueDate;
		this.checkedOutBy = checkedOutBy;
	}

	public void returnOn(LocalDate returnDate, StaffAccount returnedBy) {
		this.returnDate = returnDate;
		this.returnedBy = returnedBy;
		this.activeCopyId = null;
	}

	public void renewUntil(LocalDate newDueDate, StaffAccount renewedBy) {
		this.dueDate = newDueDate;
		this.renewedAt = Instant.now();
		this.renewedBy = renewedBy;
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public BookCopy getBookCopy() {
		return bookCopy;
	}

	public LocalDate getCheckoutDate() {
		return checkoutDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public LocalDate getReturnDate() {
		return returnDate;
	}

	public Instant getRenewedAt() {
		return renewedAt;
	}

	public StaffAccount getRenewedBy() {
		return renewedBy;
	}

	public StaffAccount getCheckedOutBy() {
		return checkedOutBy;
	}

	public StaffAccount getReturnedBy() {
		return returnedBy;
	}
}
