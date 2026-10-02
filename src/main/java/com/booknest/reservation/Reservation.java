package com.booknest.reservation;

import java.time.Instant;

import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {

	public enum Status {
		WAITING,
		HELD,
		CANCELLED,
		FULFILLED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "copy_id")
	private BookCopy copy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Status status;

	@Column(name = "active_member_book_key", length = 64, unique = true)
	private String activeMemberBookKey;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Reservation() {
	}

	public Reservation(Member member, Book book) {
		this.member = member;
		this.book = book;
		this.status = Status.WAITING;
		this.activeMemberBookKey = activeKey(member.getId(), book.getId());
	}

	public void hold(BookCopy copy) {
		this.copy = copy;
		this.status = Status.HELD;
	}

	public void cancel() {
		this.copy = null;
		this.status = Status.CANCELLED;
		this.activeMemberBookKey = null;
	}

	public void fulfill() {
		this.copy = null;
		this.status = Status.FULFILLED;
		this.activeMemberBookKey = null;
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Book getBook() {
		return book;
	}

	public BookCopy getCopy() {
		return copy;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public static String activeKey(long memberId, long bookId) {
		return memberId + "-" + bookId;
	}
}
