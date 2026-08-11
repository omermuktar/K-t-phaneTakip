package com.omermuktar.library.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

  private static final long serialVersionUID = 1L;

  public ConflictException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }
}
