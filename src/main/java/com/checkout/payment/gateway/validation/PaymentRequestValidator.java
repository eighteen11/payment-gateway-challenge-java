package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.FieldError;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestValidator {

  public void validate(PostPaymentRequest request) {
    List<FieldError> errors = new ArrayList<>();
    validateExpiryDate(request.getExpiryMonth(), request.getExpiryYear(), errors);

    if (!errors.isEmpty()) {
      throw new ApiException(ApiErrorSpec.VALIDATION_ERROR,
          "Payment request validation failed", errors);
    }
  }

  private void validateExpiryDate(Integer expiryMonth, Integer expiryYear, List<FieldError> errors) {
    if (expiryMonth == null || expiryYear == null) {
      return;
    }
    if (expiryMonth < 1 || expiryMonth > 12) {
      return;
    }
    try {
      YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
      if (expiry.isBefore(YearMonth.now())) {
        errors.add(new FieldError("expiry_date", "Card expiry date must be in the future"));
      }
    } catch (Exception e) {
      errors.add(new FieldError("expiry_year", "Invalid expiry year"));
    }
  }
}
