package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostPaymentRequestValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void whenRequestIsValidThenNoViolations() {
    assertTrue(validator.validate(validRequest()).isEmpty());
  }

  @Test
  void whenCardNumberIsMissingThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(null);

    assertFieldError(request, "card_number", "Card number is required");
  }

  @Test
  void whenCardNumberIsTooShortThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4111111111111");

    assertFieldError(request, "card_number", "Card number must be 14 to 19 digits");
  }

  @Test
  void whenCardNumberIsTooLongThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("41111111111111111111");

    assertFieldError(request, "card_number", "Card number must be 14 to 19 digits");
  }

  @ParameterizedTest
  @ValueSource(strings = {"411111111111111", "4111111111111111", "4111111111111111111"})
  void whenCardNumberLengthIsValidThenAccepted(String cardNumber) {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(cardNumber);

    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void whenCardNumberContainsNonDigitsThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4111-1111-1111-1111");

    assertFieldError(request, "card_number", "Card number must be 14 to 19 digits");
  }

  @Test
  void whenExpiryMonthIsMissingThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(null);

    assertFieldError(request, "expiry_month", "Expiry month is required");
  }

  @Test
  void whenExpiryMonthIsInvalidThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(13);

    assertFieldError(request, "expiry_month", "Expiry month must be between 1 and 12");
  }

  @Test
  void whenExpiryYearIsMissingThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setExpiryYear(null);

    assertFieldError(request, "expiry_year", "Expiry year is required");
  }

  @Test
  void whenCurrencyIsMissingThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCurrency(null);

    assertFieldError(request, "currency", "Currency is required");
  }

  @Test
  void whenCurrencyIsNotUppercaseThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCurrency("usd");

    assertFieldError(request, "currency", "Currency must be exactly 3 uppercase letters");
  }

  @Test
  void whenCurrencyIsNotSupportedThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCurrency("JPY");

    assertFieldError(request, "currency", "Currency must be one of: USD, GBP, EUR");
  }

  @Test
  void whenAmountIsMissingThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setAmount(null);

    assertFieldError(request, "amount", "Amount is required");
  }

  @Test
  void whenAmountIsZeroThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setAmount(0);

    assertFieldError(request, "amount", "Amount must be a positive integer");
  }

  @Test
  void whenCvvIsMissingThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCvv(null);

    assertFieldError(request, "cvv", "CVV is required");
  }

  @Test
  void whenCvvIsTooShortThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCvv("12");

    assertFieldError(request, "cvv", "CVV must be 3 or 4 digits");
  }

  @Test
  void whenCvvIsTooLongThenRejected() {
    PostPaymentRequest request = validRequest();
    request.setCvv("12345");

    assertFieldError(request, "cvv", "CVV must be 3 or 4 digits");
  }

  private PostPaymentRequest validRequest() {
    YearMonth future = YearMonth.now().plusMonths(6);
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(future.getMonthValue());
    request.setExpiryYear(future.getYear());
    request.setCurrency("USD");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

  private void assertFieldError(PostPaymentRequest request, String field, String message) {
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    long matches = violations.stream()
        .filter(v -> field.equals(toJsonFieldName(v)) && message.equals(v.getMessage()))
        .count();
    assertEquals(1, matches,
        () -> "Expected field=%s message=%s but got violations: %s".formatted(field, message,
            violations));
  }

  private String toJsonFieldName(ConstraintViolation<PostPaymentRequest> violation) {
    return ValidationFieldNames.toJsonFieldName(violation.getPropertyPath().toString());
  }
}
