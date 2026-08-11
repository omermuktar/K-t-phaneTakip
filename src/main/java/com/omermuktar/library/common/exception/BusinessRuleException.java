package com.omermuktar.library.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DomainException {

  private static final long serialVersionUID = 1L;

  public BusinessRuleException(String code, String message) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
  }
}
