package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class DefaultAcquiringBankClient implements AcquiringBankClient {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAcquiringBankClient.class);

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public DefaultAcquiringBankClient(RestTemplate restTemplate,
      @Value("${acquiring-bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  @Override
  public PaymentStatus submitPayment(PostPaymentRequest request) {
    BankPaymentRequest bankRequest = toBankRequest(request);
    LOG.info("Submitting payment to acquiring bank: currency={}, amount={}",
        bankRequest.getCurrency(), bankRequest.getAmount());

    try {
      ResponseEntity<BankPaymentResponse> response = restTemplate.exchange(
          bankUrl + "/payments",
          HttpMethod.POST,
          new HttpEntity<>(bankRequest),
          BankPaymentResponse.class
      );

      BankPaymentResponse body = response.getBody();
      if (body == null) {
        throw new ApiException(ApiErrorSpec.BANK_UNAVAILABLE,
            "Acquiring bank returned an empty response");
      }

      PaymentStatus status = body.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
      LOG.info("Acquiring bank response: status={}", status.getName());
      return status;
    } catch (HttpServerErrorException e) {
      LOG.error("Acquiring bank unavailable: status={}", e.getStatusCode().value());
      throw bankUnavailable();
    } catch (ResourceAccessException e) {
      LOG.error("Acquiring bank unreachable: url={}", bankUrl + "/payments", e);
      throw bankUnavailable();
    }
  }

  private static ApiException bankUnavailable() {
    return new ApiException(ApiErrorSpec.BANK_UNAVAILABLE,
        "Acquiring bank is currently unavailable");
  }

  static BankPaymentRequest toBankRequest(PostPaymentRequest request) {
    String expiryDate = String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear());
    return new BankPaymentRequest(
        request.getCardNumber(),
        expiryDate,
        request.getCurrency(),
        request.getAmount(),
        request.getCvv()
    );
  }
}
