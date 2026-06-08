package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;

public enum ApiErrorSpec {

  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request is invalid"),
  BANK_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "BANK_UNAVAILABLE", "Acquiring bank is down"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "Payment not found"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");

  public final HttpStatus status;
  public final String code;
  public final String description;

  ApiErrorSpec(HttpStatus status, String code, String description) {
    this.status = status;
    this.code = code;
    this.description = description;
  }
}
