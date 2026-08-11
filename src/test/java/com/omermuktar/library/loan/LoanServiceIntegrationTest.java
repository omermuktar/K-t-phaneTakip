package com.omermuktar.library.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omermuktar.library.book.Book;
import com.omermuktar.library.book.BookRepository;
import com.omermuktar.library.common.exception.BusinessRuleException;
import com.omermuktar.library.common.exception.ConflictException;
import com.omermuktar.library.dashboard.DashboardService;
import com.omermuktar.library.dashboard.dto.DashboardSummaryResponse;
import com.omermuktar.library.loan.dto.LoanCreateRequest;
import com.omermuktar.library.loan.dto.LoanResponse;
import com.omermuktar.library.member.Member;
import com.omermuktar.library.member.MemberRepository;
import com.omermuktar.library.member.MemberService;
import com.omermuktar.library.member.dto.MemberUpdateRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(LoanServiceIntegrationTest.FixedClockConfig.class)
@Transactional
class LoanServiceIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-08-10T09:00:00Z");

  private final LoanService loanService;
  private final LoanRepository loanRepository;
  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;
  private final MemberService memberService;
  private final DashboardService dashboardService;

  private Book book;
  private Member member;

  @Autowired
  LoanServiceIntegrationTest(
      LoanService loanService,
      LoanRepository loanRepository,
      BookRepository bookRepository,
      MemberRepository memberRepository,
      MemberService memberService,
      DashboardService dashboardService) {
    this.loanService = loanService;
    this.loanRepository = loanRepository;
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
    this.memberService = memberService;
    this.dashboardService = dashboardService;
  }

  @BeforeEach
  void setUp() {
    book =
        bookRepository.save(
            Book.create("9780132350884", "Clean Code", "Robert C. Martin", "Software", 1));
    member = memberRepository.save(Member.create("MEM-100", "Ömer Muktar", "omer@example.com"));
  }

  @Test
  void borrowingCreatesActiveLoanAndDecrementsAvailableCopies() {
    LoanResponse response =
        loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14));

    assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
    assertThat(response.borrowedAt()).isEqualTo(NOW);
    assertThat(response.dueAt()).isEqualTo(NOW.plusSeconds(14L * 24 * 60 * 60));
    assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies()).isZero();
  }

  @Test
  void returningLoanRestoresAvailableCopy() {
    LoanResponse borrowed =
        loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 7));

    LoanResponse returned = loanService.returnLoan(borrowed.id());

    assertThat(returned.status()).isEqualTo(LoanStatus.RETURNED);
    assertThat(returned.returnedAt()).isEqualTo(NOW);
    assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies())
        .isEqualTo(1);
  }

  @Test
  void unavailableBookCannotBeBorrowed() {
    Member secondMember =
        memberRepository.save(Member.create("MEM-101", "Grace Hopper", "grace@example.com"));
    loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14));

    assertThatThrownBy(
            () -> loanService.borrow(new LoanCreateRequest(book.getId(), secondMember.getId(), 14)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("kullanılabilir kopya");
  }

  @Test
  void sameMemberCannotBorrowSameBookTwice() {
    book.updateDetails(book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCategory(), 2);
    loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14));

    assertThatThrownBy(
            () -> loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("zaten aktif");
  }

  @Test
  void activeBorrowerCannotBeDeactivated() {
    loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14));

    MemberUpdateRequest request =
        new MemberUpdateRequest(
            member.getMembershipNumber(), member.getFullName(), member.getEmail(), false);

    assertThatThrownBy(() -> memberService.update(member.getId(), request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("pasif");
  }

  @Test
  void returnedLoanCannotBeReturnedAgain() {
    LoanResponse borrowed =
        loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14));
    loanService.returnLoan(borrowed.id());

    assertThatThrownBy(() -> loanService.returnLoan(borrowed.id()))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("daha önce iade");
  }

  @Test
  void overdueLoansAreCalculatedFromCurrentTime() {
    book.borrowCopy();
    Loan overdueLoan =
        Loan.create(book, member, NOW.minus(30, ChronoUnit.DAYS), NOW.minus(16, ChronoUnit.DAYS));
    loanRepository.saveAndFlush(overdueLoan);

    LoanResponse response = loanService.findOverdue(PageRequest.of(0, 10)).getContent().get(0);

    assertThat(response.overdue()).isTrue();
    assertThat(response.daysOverdue()).isEqualTo(16);
  }

  @Test
  void dashboardReflectsCurrentInventoryAndLoanCounts() {
    loanService.borrow(new LoanCreateRequest(book.getId(), member.getId(), 14));

    DashboardSummaryResponse summary = dashboardService.getSummary();

    assertThat(summary.totalBookTitles()).isEqualTo(1);
    assertThat(summary.totalCopies()).isEqualTo(1);
    assertThat(summary.availableCopies()).isZero();
    assertThat(summary.activeMembers()).isEqualTo(1);
    assertThat(summary.activeLoans()).isEqualTo(1);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfig {

    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
