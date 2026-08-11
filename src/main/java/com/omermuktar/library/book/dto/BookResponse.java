package com.omermuktar.library.book.dto;

import com.omermuktar.library.book.Book;
import java.time.Instant;

public record BookResponse(
    Long id,
    String isbn,
    String title,
    String author,
    String category,
    int totalCopies,
    int availableCopies,
    boolean available,
    Instant createdAt,
    Instant updatedAt) {
  public static BookResponse from(Book book) {
    return new BookResponse(
        book.getId(),
        book.getIsbn(),
        book.getTitle(),
        book.getAuthor(),
        book.getCategory(),
        book.getTotalCopies(),
        book.getAvailableCopies(),
        book.getAvailableCopies() > 0,
        book.getCreatedAt(),
        book.getUpdatedAt());
  }
}
