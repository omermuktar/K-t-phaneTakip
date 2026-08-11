package com.omermuktar.library.book;

import com.omermuktar.library.common.exception.BusinessRuleException;
import com.omermuktar.library.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "books",
    uniqueConstraints = @UniqueConstraint(name = "uq_books_isbn", columnNames = "isbn"))
public class Book extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String isbn;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, length = 120)
  private String author;

  @Column(nullable = false, length = 80)
  private String category;

  @Column(name = "total_copies", nullable = false)
  private int totalCopies;

  @Column(name = "available_copies", nullable = false)
  private int availableCopies;

  @Version
  @Column(nullable = false)
  private long version;

  protected Book() {}

  private Book(String isbn, String title, String author, String category, int totalCopies) {
    this.isbn = normalizeIsbn(isbn);
    this.title = title.trim();
    this.author = author.trim();
    this.category = category.trim();
    this.totalCopies = totalCopies;
    this.availableCopies = totalCopies;
  }

  public static Book create(
      String isbn, String title, String author, String category, int totalCopies) {
    return new Book(isbn, title, author, category, totalCopies);
  }

  public void updateDetails(
      String isbn, String title, String author, String category, int newTotalCopies) {
    int borrowedCopies = totalCopies - availableCopies;
    if (newTotalCopies < borrowedCopies) {
      throw new BusinessRuleException(
          "COPY_COUNT_BELOW_ACTIVE_LOANS",
          "Toplam kopya sayısı ödünçteki kopya sayısından az olamaz.");
    }
    this.isbn = normalizeIsbn(isbn);
    this.title = title.trim();
    this.author = author.trim();
    this.category = category.trim();
    this.totalCopies = newTotalCopies;
    this.availableCopies = newTotalCopies - borrowedCopies;
  }

  public void borrowCopy() {
    if (availableCopies <= 0) {
      throw new BusinessRuleException(
          "BOOK_UNAVAILABLE", "Bu kitap için kullanılabilir kopya bulunmuyor.");
    }
    availableCopies--;
  }

  public void returnCopy() {
    if (availableCopies >= totalCopies) {
      throw new BusinessRuleException(
          "COPY_COUNT_INCONSISTENT", "Kitabın tüm kopyaları zaten kütüphanede.");
    }
    availableCopies++;
  }

  private static String normalizeIsbn(String isbn) {
    return isbn.replace("-", "").trim().toUpperCase();
  }

  public Long getId() {
    return id;
  }

  public String getIsbn() {
    return isbn;
  }

  public String getTitle() {
    return title;
  }

  public String getAuthor() {
    return author;
  }

  public String getCategory() {
    return category;
  }

  public int getTotalCopies() {
    return totalCopies;
  }

  public int getAvailableCopies() {
    return availableCopies;
  }

  public long getVersion() {
    return version;
  }
}
