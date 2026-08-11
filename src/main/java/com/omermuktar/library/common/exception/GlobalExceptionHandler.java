package com.omermuktar.library.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final Map<HttpStatus, String> TURKISH_TITLES =
      Map.of(
          HttpStatus.BAD_REQUEST, "Hatalı İstek",
          HttpStatus.NOT_FOUND, "Kayıt Bulunamadı",
          HttpStatus.CONFLICT, "İşlem Çakışması",
          HttpStatus.UNPROCESSABLE_CONTENT, "İş Kuralı İhlali",
          HttpStatus.INTERNAL_SERVER_ERROR, "Sunucu Hatası");

  @ExceptionHandler(DomainException.class)
  ProblemDetail handleDomainException(DomainException exception, HttpServletRequest request) {
    return problem(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<Map<String, String>> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error -> {
                  Map<String, String> violation = new LinkedHashMap<>();
                  violation.put("field", error.getField());
                  violation.put("message", error.getDefaultMessage());
                  return violation;
                })
            .toList();
    return problem(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Gönderilen bilgiler doğrulama kurallarını karşılamıyor.",
        request,
        violations);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<Map<String, String>> violations =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
            .toList();
    return problem(
        HttpStatus.BAD_REQUEST,
        "CONSTRAINT_VIOLATION",
        "İstek parametreleri geçersiz.",
        request,
        violations);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail handleMalformedRequest(Exception exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST",
        "İstek gövdesi veya parametre biçimi geçersiz.",
        request,
        null);
  }

  @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
  ProblemDetail handleInvalidQuery(Exception exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "INVALID_QUERY",
        "Filtreleme veya sıralama parametresi geçersiz.",
        request,
        null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    log.warn("Database constraint violation", exception);
    return problem(
        HttpStatus.CONFLICT,
        "DATA_INTEGRITY_VIOLATION",
        "İşlem veri bütünlüğü kuralıyla çakıştı.",
        request,
        null);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ProblemDetail handleConcurrentUpdate(
      ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "CONCURRENT_UPDATE",
        "Kayıt başka bir işlem tarafından güncellendi. Lütfen veriyi yenileyip tekrar deneyin.",
        request,
        null);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
    log.error("Unexpected request failure", exception);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Beklenmeyen bir hata oluştu.",
        request,
        null);
  }

  private ProblemDetail problem(
      HttpStatus status,
      String code,
      String detail,
      HttpServletRequest request,
      Object violations) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(turkishTitle(status));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    problem.setProperty("timestamp", Instant.now());
    if (violations != null) {
      problem.setProperty("violations", violations);
    }
    return problem;
  }

  private String turkishTitle(HttpStatus status) {
    return TURKISH_TITLES.getOrDefault(status, "İstek Hatası");
  }
}
