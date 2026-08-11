package com.omermuktar.library.loan;

import com.omermuktar.library.book.Book;
import com.omermuktar.library.book.BookRepository;
import com.omermuktar.library.common.exception.BusinessRuleException;
import com.omermuktar.library.common.exception.ConflictException;
import com.omermuktar.library.common.exception.ResourceNotFoundException;
import com.omermuktar.library.loan.dto.LoanCreateRequest;
import com.omermuktar.library.loan.dto.LoanResponse;
import com.omermuktar.library.member.Member;
import com.omermuktar.library.member.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LoanService {

  private final LoanRepository loanRepository;
  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;
  private final Clock clock;

  public LoanService(
      LoanRepository loanRepository,
      BookRepository bookRepository,
      MemberRepository memberRepository,
      Clock clock) {
    this.loanRepository = loanRepository;
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
    this.clock = clock;
  }

  @Transactional
  public LoanResponse borrow(LoanCreateRequest request) {
    Book book =
        bookRepository
            .findByIdForUpdate(request.bookId())
            .orElseThrow(() -> new ResourceNotFoundException("Kitap", request.bookId()));
    Member member =
        memberRepository
            .findByIdForUpdate(request.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Üye", request.memberId()));

    if (!member.isActive()) {
      throw new BusinessRuleException("MEMBER_INACTIVE", "Pasif üyeye kitap ödünç verilemez.");
    }
    if (loanRepository.existsByMemberIdAndBookIdAndStatus(
        member.getId(), book.getId(), LoanStatus.ACTIVE)) {
      throw new ConflictException(
          "DUPLICATE_ACTIVE_LOAN", "Üyenin bu kitap için zaten aktif bir ödünç kaydı var.");
    }

    book.borrowCopy();
    Instant borrowedAt = clock.instant();
    Instant dueAt = borrowedAt.plus(request.effectiveLoanDays(), ChronoUnit.DAYS);
    Loan loan = loanRepository.saveAndFlush(Loan.create(book, member, borrowedAt, dueAt));
    return LoanResponse.from(loan, borrowedAt);
  }

  @Transactional
  public LoanResponse returnLoan(Long loanId) {
    Loan loan =
        loanRepository
            .findByIdForUpdate(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Ödünç kaydı", loanId));
    Book book =
        bookRepository
            .findByIdForUpdate(loan.getBook().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Kitap", loan.getBook().getId()));

    Instant returnedAt = clock.instant();
    loan.markReturned(returnedAt);
    book.returnCopy();
    loanRepository.flush();
    return LoanResponse.from(loan, returnedAt);
  }

  public LoanResponse getById(Long id) {
    Loan loan =
        loanRepository
            .findDetailedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ödünç kaydı", id));
    return LoanResponse.from(loan, clock.instant());
  }

  public Page<LoanResponse> search(
      LoanStatus status, Long memberId, Long bookId, Pageable pageable) {
    Instant now = clock.instant();
    return loanRepository
        .search(status, memberId, bookId, pageable)
        .map(loan -> LoanResponse.from(loan, now));
  }

  public Page<LoanResponse> findOverdue(Pageable pageable) {
    Instant now = clock.instant();
    return loanRepository
        .findByStatusAndDueAtBefore(LoanStatus.ACTIVE, now, pageable)
        .map(loan -> LoanResponse.from(loan, now));
  }
}
