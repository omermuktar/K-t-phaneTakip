package com.omermuktar.library.dashboard;

import com.omermuktar.library.book.BookRepository;
import com.omermuktar.library.dashboard.dto.DashboardSummaryResponse;
import com.omermuktar.library.loan.LoanRepository;
import com.omermuktar.library.loan.LoanStatus;
import com.omermuktar.library.member.MemberRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;
  private final LoanRepository loanRepository;
  private final Clock clock;

  public DashboardService(
      BookRepository bookRepository,
      MemberRepository memberRepository,
      LoanRepository loanRepository,
      Clock clock) {
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
    this.loanRepository = loanRepository;
    this.clock = clock;
  }

  public DashboardSummaryResponse getSummary() {
    return new DashboardSummaryResponse(
        bookRepository.count(),
        bookRepository.countByAvailableCopiesGreaterThan(0),
        bookRepository.sumTotalCopies(),
        bookRepository.sumAvailableCopies(),
        memberRepository.countByActiveTrue(),
        loanRepository.countByStatus(LoanStatus.ACTIVE),
        loanRepository.countByStatusAndDueAtBefore(LoanStatus.ACTIVE, clock.instant()));
  }
}
