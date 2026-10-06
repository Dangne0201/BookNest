package com.booknest.activity;

import java.time.Instant;

import com.booknest.book.Book;
import com.booknest.book.BookCopy;
import com.booknest.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_events")
public class ActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private ActivityEventType type;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt = Instant.now();

	@Column(name = "actor_username", nullable = false, updatable = false, length = 50)
	private String actorUsername;

	@Column(name = "patron_account_id", updatable = false)
	private Long patronAccountId;

	@Column(name = "member_id", updatable = false)
	private Long memberId;

	@Column(name = "member_name", updatable = false, length = 150)
	private String memberName;

	@Column(name = "book_id", updatable = false)
	private Long bookId;

	@Column(name = "book_title", updatable = false, length = 200)
	private String bookTitle;

	@Column(name = "copy_id", updatable = false)
	private Long copyId;

	@Column(name = "loan_id", updatable = false)
	private Long loanId;

	@Column(name = "reservation_id", updatable = false)
	private Long reservationId;

	protected ActivityEvent() {
	}

	public ActivityEvent(
			ActivityEventType type,
			String actorUsername,
			Member member,
			Book book,
			BookCopy copy,
			Long loanId,
			Long reservationId
	) {
		this.type = type;
		this.actorUsername = actorUsername;
		if (member != null) {
			this.memberId = member.getId();
			this.memberName = member.getFullName();
			this.patronAccountId = member.getAccount() == null ? null : member.getAccount().getId();
		}
		Book resolvedBook = book != null ? book : copy == null ? null : copy.getBook();
		if (resolvedBook != null) {
			this.bookId = resolvedBook.getId();
			this.bookTitle = resolvedBook.getTitle();
		}
		this.copyId = copy == null ? null : copy.getId();
		this.loanId = loanId;
		this.reservationId = reservationId;
	}

	public Long getId() {
		return id;
	}

	public ActivityEventType getType() {
		return type;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getActorUsername() {
		return actorUsername;
	}

	public Long getPatronAccountId() {
		return patronAccountId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getMemberName() {
		return memberName;
	}

	public Long getBookId() {
		return bookId;
	}

	public String getBookTitle() {
		return bookTitle;
	}

	public Long getCopyId() {
		return copyId;
	}

	public Long getLoanId() {
		return loanId;
	}

	public Long getReservationId() {
		return reservationId;
	}
}
