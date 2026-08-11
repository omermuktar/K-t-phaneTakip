package com.omermuktar.library.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String resource, Object id) {
    super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resource + " bulunamadı: " + id);
  }
}
