package com.booknest.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "books", uniqueConstraints = @UniqueConstraint(name = "uk_books_isbn", columnNames = "isbn"))
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 150)
	private String author;

	@Column(length = 13)
	private String isbn;

	@Column(length = 100)
	private String genre;

	@Column(name = "publication_year")
	private Integer publicationYear;

	@Column(length = 2000)
	private String description;

	protected Book() {
	}

	public Book(
			String title,
			String author,
			String isbn,
			String genre,
			Integer publicationYear,
			String description
	) {
		this.title = title;
		this.author = author;
		this.isbn = isbn;
		this.genre = genre;
		this.publicationYear = publicationYear;
		this.description = description;
	}

	public void updateDetails(
			String title,
			String author,
			String isbn,
			String genre,
			Integer publicationYear,
			String description
	) {
		this.title = title;
		this.author = author;
		this.isbn = isbn;
		this.genre = genre;
		this.publicationYear = publicationYear;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getGenre() {
		return genre;
	}

	public Integer getPublicationYear() {
		return publicationYear;
	}

	public String getDescription() {
		return description;
	}
}
