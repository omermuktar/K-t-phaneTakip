package com.omermuktar.library.loan;

import com.omermuktar.library.book.Book;
import com.omermuktar.library.common.exception.ConflictException;
import com.omermuktar.library.common.persistence.AuditableEntity;
import com.omermuktar.library.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(
    name = "loans",
    indexes = {
      @Index(name = "idx_loans_status_due_at", columnList = "status,due_at"),
      @Index(name = "idx_loans_member", columnList = "member_id"),
      @Index(name = "idx_loans_book", columnList = "book_id")
    })
public class Loan extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "borrowed_at", nullable = false)
  private Instant borrowedAt;

  @Column(name = "due_at", nullable = false)
  private Instant dueAt;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LoanStatus status;

  @Version
  @Column(nullable = false)
  private long version;

  protected Loan() {}

  private Loan(Book book, Member member, Instant borrowedAt, Instant dueAt) {
    this.book = book;
    this.member = member;
    this.borrowedAt = borrowedAt;
    this.dueAt = dueAt;
    this.status = LoanStatus.ACTIVE;
  }

  public static Loan create(Book book, Member member, Instant borrowedAt, Instant dueAt) {
    return new Loan(book, member, borrowedAt, dueAt);
  }

  public void markReturned(Instant returnedAt) {
    if (status == LoanStatus.RETURNED) {
      throw new ConflictException(
          "LOAN_ALREADY_RETURNED", "Bu ödünç kaydı daha önce iade edilmiş.");
    }
    this.status = LoanStatus.RETURNED;
    this.returnedAt = returnedAt;
  }

  public Long getId() {
    return id;
  }

  public Book getBook() {
    return book;
  }

  public Member getMember() {
    return member;
  }

  public Instant getBorrowedAt() {
    return borrowedAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }

  public LoanStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }
}
