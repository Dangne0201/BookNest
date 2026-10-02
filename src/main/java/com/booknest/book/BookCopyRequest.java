package com.booknest.book;

import jakarta.validation.constraints.NotNull;

public record BookCopyRequest(@NotNull BookCopy.Status status) {
}
