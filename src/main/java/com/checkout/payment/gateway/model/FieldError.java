package com.checkout.payment.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single field-level validation error")
public class FieldError {

  @Schema(description = "JSON field name", example = "card_number")
  private final String field;

  @Schema(description = "Validation message", example = "Card number is required")
  private final String message;

  public FieldError(String field, String message) {
    this.field = field;
    this.message = message;
  }

  public String getField() {
    return field;
  }

  public String getMessage() {
    return message;
  }
}
