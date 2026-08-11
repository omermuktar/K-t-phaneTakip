package com.omermuktar.library.loan.dto;

import com.omermuktar.library.loan.Loan;
import com.omermuktar.library.loan.LoanStatus;
import java.time.Duration;
import java.time.Instant;

public record LoanResponse(
    Long id,
    BookSummary book,
    MemberSummary member,
    Instant borrowedAt,
    Instant dueAt,
    Instant returnedAt,
    LoanStatus status,
    boolean overdue,
    long daysOverdue,
    Instant createdAt,
    Instant updatedAt) {
  public static LoanResponse from(Loan loan, Instant now) {
    boolean overdue = loan.getStatus() == LoanStatus.ACTIVE && loan.getDueAt().isBefore(now);
    long daysOverdue = overdue ? Math.max(1, Duration.between(loan.getDueAt(), now).toDays()) : 0;
    return new LoanResponse(
        loan.getId(),
        new BookSummary(
            loan.getBook().getId(), loan.getBook().getIsbn(), loan.getBook().getTitle()),
        new MemberSummary(
            loan.getMember().getId(),
            loan.getMember().getMembershipNumber(),
            loan.getMember().getFullName()),
        loan.getBorrowedAt(),
        loan.getDueAt(),
        loan.getReturnedAt(),
        loan.getStatus(),
        overdue,
        daysOverdue,
        loan.getCreatedAt(),
        loan.getUpdatedAt());
  }

  public record BookSummary(Long id, String isbn, String title) {}

  public record MemberSummary(Long id, String membershipNumber, String fullName) {}
}
