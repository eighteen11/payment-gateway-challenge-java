package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

@Schema(description = "Card payment request")
public class PostPaymentRequest implements Serializable {

  @Schema(description = "Full card number (14–19 digits)", example = "4111111111111111")
  @NotBlank(message = "Card number is required")
  @Pattern(regexp = "\\d{14,19}", message = "Card number must be 14 to 19 digits")
  @JsonProperty("card_number")
  private String cardNumber;

  @Schema(description = "Expiry month (1–12)", example = "12")
  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @Schema(description = "Expiry year", example = "2028")
  @NotNull(message = "Expiry year is required")
  @JsonProperty("expiry_year")
  private Integer expiryYear;

  @Schema(description = "ISO 4217 currency code", example = "USD", allowableValues = {"USD", "GBP", "EUR"})
  @NotBlank(message = "Currency is required")
  @Pattern(regexp = "[A-Z]{3}", message = "Currency must be exactly 3 uppercase letters")
  @Pattern(regexp = "USD|GBP|EUR", message = "Currency must be one of: USD, GBP, EUR")
  private String currency;

  @Schema(description = "Amount in minor units (e.g. cents)", example = "100")
  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be a positive integer")
  private Integer amount;

  @Schema(description = "Card verification value (3–4 digits)", example = "123")
  @NotBlank(message = "CVV is required")
  @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3 or 4 digits")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber=[REDACTED]" +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=[REDACTED]" +
        '}';
  }
}
