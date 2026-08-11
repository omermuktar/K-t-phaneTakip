package com.omermuktar.library.loan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LoanCreateRequest(
    @NotNull(message = "Kitap kimliği boş bırakılamaz.")
        @Positive(message = "Kitap kimliği pozitif bir sayı olmalıdır.")
        Long bookId,
    @NotNull(message = "Üye kimliği boş bırakılamaz.")
        @Positive(message = "Üye kimliği pozitif bir sayı olmalıdır.")
        Long memberId,
    @Min(value = 1, message = "Ödünç süresi en az 1 gün olmalıdır.")
        @Max(value = 60, message = "Ödünç süresi en fazla 60 gün olabilir.")
        Integer loanDays) {
  public int effectiveLoanDays() {
    return loanDays == null ? 14 : loanDays;
  }
}
