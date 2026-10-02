package com.booknest.book;

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
@Table(name = "book_copies")
public class BookCopy {

	public enum Status {
		AVAILABLE,
		ON_LOAN,
		ON_HOLD,
		MAINTENANCE,
		RETIRED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	protected BookCopy() {
	}

	public BookCopy(Book book, Status status) {
		this.book = book;
		this.status = status;
	}

	public void updateStatus(Status status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public Book getBook() {
		return book;
	}

	public Status getStatus() {
		return status;
	}
}
