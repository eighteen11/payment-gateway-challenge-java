package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.FieldError;
import java.util.Collections;
import java.util.List;

public class ApiException extends RuntimeException {

  private final ApiErrorSpec spec;
  private final List<FieldError> errors;

  public ApiException(ApiErrorSpec spec) {
    this(spec, spec.description, Collections.emptyList());
  }

  public ApiException(ApiErrorSpec spec, String message) {
    this(spec, message, Collections.emptyList());
  }

  public ApiException(ApiErrorSpec spec, String message, List<FieldError> errors) {
    super(message);
    this.spec = spec;
    this.errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
  }

  public ApiErrorSpec getSpec() {
    return spec;
  }

  public List<FieldError> getErrors() {
    return errors;
  }
}
