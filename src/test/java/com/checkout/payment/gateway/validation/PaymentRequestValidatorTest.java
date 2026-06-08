package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.FieldError;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  private PaymentRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentRequestValidator();
  }

  @Test
  void whenExpiryDateIsInTheFutureThenAccepted() {
    PostPaymentRequest request = validRequest();

    assertDoesNotThrow(() -> validator.validate(request));
  }

  @Test
  void whenExpiryDateIsCurrentMonthThenAccepted() {
    PostPaymentRequest request = validRequest();
    YearMonth current = YearMonth.now();
    request.setExpiryMonth(current.getMonthValue());
    request.setExpiryYear(current.getYear());

    assertDoesNotThrow(() -> validator.validate(request));
  }

  @Test
  void whenExpiryDateIsInThePastThenRejected() {
    PostPaymentRequest request = validRequest();
    YearMonth past = YearMonth.now().minusMonths(1);
    request.setExpiryMonth(past.getMonthValue());
    request.setExpiryYear(past.getYear());

    ApiException ex = assertThrows(ApiException.class, () -> validator.validate(request));

    assertEquals(ApiErrorSpec.VALIDATION_ERROR, ex.getSpec());
    assertFieldError(ex.getErrors(), "expiry_date", "Card expiry date must be in the future");
  }

  @Test
  void whenExpiryYearIsInvalidThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setExpiryYear(1_000_000_000);

    ApiException ex = assertThrows(ApiException.class, () -> validator.validate(request));

    assertEquals(ApiErrorSpec.VALIDATION_ERROR, ex.getSpec());
    assertFieldError(ex.getErrors(), "expiry_year", "Invalid expiry year");
  }

  private PostPaymentRequest validRequest() {
    YearMonth future = YearMonth.now().plusMonths(6);
    PostPaymentRequest request = new PostPaymentRequest();
    request.setExpiryMonth(future.getMonthValue());
    request.setExpiryYear(future.getYear());
    return request;
  }

  private void assertFieldError(List<FieldError> errors, String field, String message) {
    long matches = errors.stream()
        .filter(e -> field.equals(e.getField()) && message.equals(e.getMessage()))
        .count();
    assertEquals(1, matches,
        () -> "Expected field=%s message=%s but got errors: %s".formatted(field, message, errors));
  }
}
