package com.booknest.book;

public record BookCopyResponse(Long id, Long bookId, BookCopy.Status status) {
}
