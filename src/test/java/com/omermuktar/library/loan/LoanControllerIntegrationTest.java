package com.omermuktar.library.loan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omermuktar.library.book.Book;
import com.omermuktar.library.book.BookRepository;
import com.omermuktar.library.member.Member;
import com.omermuktar.library.member.MemberRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoanControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private BookRepository bookRepository;
  @Autowired private MemberRepository memberRepository;
  @Autowired private LoanRepository loanRepository;

  private Book book;
  private Member member;

  @BeforeEach
  void setUp() {
    book =
        bookRepository.saveAndFlush(
            Book.create("9780132350884", "Clean Code", "Robert C. Martin", "Software", 2));
    member =
        memberRepository.saveAndFlush(Member.create("MEM-100", "Ömer Muktar", "omer@example.com"));
  }

  @Test
  void borrowGetSearchAndReturnFlowWorks() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(book.getId(), member.getId(), "null")))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.book.id").value(book.getId()))
        .andExpect(jsonPath("$.member.id").value(member.getId()))
        .andExpect(jsonPath("$.overdue").value(false));

    Loan loan = loanRepository.findAll().get(0);

    mockMvc
        .perform(get("/api/v1/loans/{id}", loan.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(loan.getId()));

    mockMvc
        .perform(
            get("/api/v1/loans")
                .param("status", "ACTIVE")
                .param("memberId", member.getId().toString())
                .param("bookId", book.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));

    mockMvc
        .perform(post("/api/v1/loans/{id}/return", loan.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RETURNED"))
        .andExpect(jsonPath("$.returnedAt").exists());

    mockMvc
        .perform(post("/api/v1/loans/{id}/return", loan.getId()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("İşlem Çakışması"))
        .andExpect(jsonPath("$.code").value("LOAN_ALREADY_RETURNED"));
  }

  @Test
  void inactiveMemberCannotBorrow() throws Exception {
    member.updateDetails(
        member.getMembershipNumber(), member.getFullName(), member.getEmail(), false);
    memberRepository.saveAndFlush(member);

    mockMvc
        .perform(
            post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(book.getId(), member.getId(), "14")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("MEMBER_INACTIVE"));
  }

  @Test
  void missingBookAndMemberReturnNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(999999L, member.getId(), "14")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

    mockMvc
        .perform(
            post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(book.getId(), 999999L, "14")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

    mockMvc
        .perform(get("/api/v1/loans/999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void loanHistoryProtectsBookAndMemberFromDeletion() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(book.getId(), member.getId(), "7")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(delete("/api/v1/books/{id}", book.getId()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BOOK_HAS_LOAN_HISTORY"));

    mockMvc
        .perform(delete("/api/v1/members/{id}", member.getId()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MEMBER_HAS_LOAN_HISTORY"));
  }

  @Test
  void overdueEndpointAndDashboardReflectStoredData() throws Exception {
    Instant now = Instant.now();
    book.borrowCopy();
    loanRepository.saveAndFlush(
        Loan.create(book, member, now.minus(30, ChronoUnit.DAYS), now.minus(15, ChronoUnit.DAYS)));

    mockMvc
        .perform(get("/api/v1/loans/overdue"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].overdue").value(true));

    mockMvc
        .perform(get("/api/v1/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalBookTitles").value(1))
        .andExpect(jsonPath("$.totalCopies").value(2))
        .andExpect(jsonPath("$.availableCopies").value(1))
        .andExpect(jsonPath("$.activeMembers").value(1))
        .andExpect(jsonPath("$.activeLoans").value(1))
        .andExpect(jsonPath("$.overdueLoans").value(1));
  }

  @Test
  void invalidLoanBodyAndStatusFilterReturnStandardErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanJson(-1L, member.getId(), "90")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    mockMvc
        .perform(get("/api/v1/loans").param("status", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  private static String loanJson(Long bookId, Long memberId, String loanDays) {
    return """
    {
      "bookId": %d,
      "memberId": %d,
      "loanDays": %s
    }
    """
        .formatted(bookId, memberId, loanDays);
  }
}
