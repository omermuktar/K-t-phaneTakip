package com.omermuktar.library.loan;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

  boolean existsByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, LoanStatus status);

  boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);

  boolean existsByMemberIdAndStatus(Long memberId, LoanStatus status);

  boolean existsByBookId(Long bookId);

  boolean existsByMemberId(Long memberId);

  long countByStatus(LoanStatus status);

  long countByStatusAndDueAtBefore(LoanStatus status, Instant dueAt);

  @EntityGraph(attributePaths = {"book", "member"})
  @Query(
      """
      select l from Loan l
      where (:status is null or l.status = :status)
        and (:memberId is null or l.member.id = :memberId)
        and (:bookId is null or l.book.id = :bookId)
      """)
  Page<Loan> search(
      @Param("status") LoanStatus status,
      @Param("memberId") Long memberId,
      @Param("bookId") Long bookId,
      Pageable pageable);

  @EntityGraph(attributePaths = {"book", "member"})
  Page<Loan> findByStatusAndDueAtBefore(LoanStatus status, Instant dueAt, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"book", "member"})
  @Query("select l from Loan l where l.id = :id")
  Optional<Loan> findByIdForUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = {"book", "member"})
  @Query("select l from Loan l where l.id = :id")
  Optional<Loan> findDetailedById(@Param("id") Long id);
}
