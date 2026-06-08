package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ApiErrorResponse;
import com.checkout.payment.gateway.model.FieldError;
import com.checkout.payment.gateway.observability.PaymentMetricsRecorder;
import com.checkout.payment.gateway.validation.ValidationFieldNames;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  private final PaymentMetricsRecorder paymentMetricsRecorder;

  public CommonExceptionHandler(PaymentMetricsRecorder paymentMetricsRecorder) {
    this.paymentMetricsRecorder = paymentMetricsRecorder;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toFieldError)
        .toList();
    LOG.warn("Validation failed ({}): {}", errors.size(), formatFieldErrors(errors));
    paymentMetricsRecorder.recordFailure(ApiErrorSpec.VALIDATION_ERROR.code);
    ApiErrorSpec spec = ApiErrorSpec.VALIDATION_ERROR;
    return ResponseEntity.status(spec.status)
        .body(new ApiErrorResponse(spec.code, "Payment request validation failed", errors));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
    if (ex.getSpec() == ApiErrorSpec.VALIDATION_ERROR) {
      LOG.warn("Validation failed ({}): {}",
          ex.getErrors().size(), formatFieldErrors(ex.getErrors()));
    } else if (ex.getSpec() == ApiErrorSpec.BANK_UNAVAILABLE) {
      LOG.error("Acquiring bank unavailable", ex);
    } else if (ex.getSpec() == ApiErrorSpec.NOT_FOUND) {
      LOG.debug("Payment not found: {}", ex.getMessage());
    } else {
      LOG.error("API error: {}", ex.getMessage(), ex);
    }
    recordFailureIfApplicable(ex.getSpec());
    return ResponseEntity.status(ex.getSpec().status)
        .body(ApiErrorResponse.from(ex));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
    LOG.error("Unexpected error", ex);
    paymentMetricsRecorder.recordFailure(ApiErrorSpec.INTERNAL_ERROR.code);
    ApiErrorSpec spec = ApiErrorSpec.INTERNAL_ERROR;
    return ResponseEntity.status(spec.status)
        .body(new ApiErrorResponse(spec.code, spec.description, null));
  }

  private void recordFailureIfApplicable(ApiErrorSpec spec) {
    if (spec == ApiErrorSpec.VALIDATION_ERROR || spec == ApiErrorSpec.BANK_UNAVAILABLE) {
      paymentMetricsRecorder.recordFailure(spec.code);
    }
  }

  private FieldError toFieldError(org.springframework.validation.FieldError springError) {
    return new FieldError(
        ValidationFieldNames.toJsonFieldName(springError.getField()),
        springError.getDefaultMessage());
  }

  private static String formatFieldErrors(List<FieldError> errors) {
    return errors.stream()
        .map(e -> e.getField() + ": " + e.getMessage())
        .collect(Collectors.joining(", "));
  }
}
