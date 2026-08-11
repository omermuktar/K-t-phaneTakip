package com.omermuktar.library.loan;

import com.omermuktar.library.common.api.PageResponse;
import com.omermuktar.library.loan.dto.LoanCreateRequest;
import com.omermuktar.library.loan.dto.LoanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/loans")
@Validated
@Tag(name = "Ödünç İşlemleri", description = "Ödünç verme, iade ve gecikme yönetimi")
public class LoanController {

  private final LoanService loanService;

  public LoanController(LoanService loanService) {
    this.loanService = loanService;
  }

  @PostMapping
  @Operation(summary = "Kitabı üyeye ödünç ver")
  public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody LoanCreateRequest request) {
    LoanResponse response = loanService.borrow(request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/{id}/return")
  @Operation(summary = "Ödünç alınan kitabı iade et")
  public LoanResponse returnLoan(
      @PathVariable @Positive(message = "Ödünç kaydı kimliği pozitif bir sayı olmalıdır.")
          Long id) {
    return loanService.returnLoan(id);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Ödünç kaydı ayrıntısını getir")
  public LoanResponse getById(
      @PathVariable @Positive(message = "Ödünç kaydı kimliği pozitif bir sayı olmalıdır.")
          Long id) {
    return loanService.getById(id);
  }

  @GetMapping
  @Operation(summary = "Ödünç kayıtlarını filtrele")
  public PageResponse<LoanResponse> search(
      @RequestParam(required = false) LoanStatus status,
      @RequestParam(required = false)
          @Positive(message = "Üye kimliği pozitif bir sayı olmalıdır.")
          Long memberId,
      @RequestParam(required = false)
          @Positive(message = "Kitap kimliği pozitif bir sayı olmalıdır.")
          Long bookId,
      @ParameterObject
          @PageableDefault(size = 20, sort = "borrowedAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return PageResponse.from(loanService.search(status, memberId, bookId, pageable));
  }

  @GetMapping("/overdue")
  @Operation(summary = "Gecikmiş ödünç kayıtlarını listele")
  public PageResponse<LoanResponse> findOverdue(
      @ParameterObject @PageableDefault(size = 20, sort = "dueAt") Pageable pageable) {
    return PageResponse.from(loanService.findOverdue(pageable));
  }
}
