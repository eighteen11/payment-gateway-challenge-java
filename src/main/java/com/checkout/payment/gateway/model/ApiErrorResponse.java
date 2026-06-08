package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "API error response")
public class ApiErrorResponse {

  @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR",
      allowableValues = {"VALIDATION_ERROR", "BANK_UNAVAILABLE", "NOT_FOUND", "INTERNAL_ERROR"})
  private final String code;

  @Schema(description = "Human-readable error message", example = "Payment request validation failed")
  private final String message;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Schema(description = "Field-level validation errors (present for VALIDATION_ERROR only)")
  private final List<FieldError> errors;

  public ApiErrorResponse(String code, String message, List<FieldError> errors) {
    this.code = code;
    this.message = message;
    this.errors = errors;
  }

  public static ApiErrorResponse from(ApiException ex) {
    return new ApiErrorResponse(ex.getSpec().code, ex.getMessage(), ex.getErrors());
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public List<FieldError> getErrors() {
    return errors;
  }
}
