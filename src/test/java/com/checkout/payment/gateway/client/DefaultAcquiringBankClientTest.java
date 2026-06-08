package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.ConnectException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DefaultAcquiringBankClientTest {

  @Mock
  private RestTemplate restTemplate;

  private DefaultAcquiringBankClient client;

  @BeforeEach
  void setUp() {
    client = new DefaultAcquiringBankClient(restTemplate, "http://localhost:8080");
  }

  @Test
  void whenBankAuthorizesThenAuthorizedStatusIsReturned() {
    PostPaymentRequest request = validRequest();
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("auth-123");

    when(restTemplate.exchange(
        eq("http://localhost:8080/payments"),
        eq(HttpMethod.POST),
        org.mockito.ArgumentMatchers.any(HttpEntity.class),
        eq(BankPaymentResponse.class)
    )).thenReturn(ResponseEntity.ok(bankResponse));

    PaymentStatus status = client.submitPayment(request);

    assertEquals(PaymentStatus.AUTHORIZED, status);
  }

  @Test
  void whenBankDeclinesThenDeclinedStatusIsReturned() {
    PostPaymentRequest request = validRequest();
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(false);

    when(restTemplate.exchange(
        eq("http://localhost:8080/payments"),
        eq(HttpMethod.POST),
        org.mockito.ArgumentMatchers.any(HttpEntity.class),
        eq(BankPaymentResponse.class)
    )).thenReturn(ResponseEntity.ok(bankResponse));

    PaymentStatus status = client.submitPayment(request);

    assertEquals(PaymentStatus.DECLINED, status);
  }

  @Test
  void whenBankIsUnreachableThenApiExceptionIsThrown() {
    PostPaymentRequest request = validRequest();

    when(restTemplate.exchange(
        eq("http://localhost:8080/payments"),
        eq(HttpMethod.POST),
        org.mockito.ArgumentMatchers.any(HttpEntity.class),
        eq(BankPaymentResponse.class)
    )).thenThrow(new ResourceAccessException(
        "I/O error on POST request for \"http://localhost:8080/payments\": Connection refused",
        new ConnectException("Connection refused")));

    ApiException ex = assertThrows(ApiException.class, () -> client.submitPayment(request));
    assertEquals(ApiErrorSpec.BANK_UNAVAILABLE, ex.getSpec());
    assertEquals("Acquiring bank is currently unavailable", ex.getMessage());
  }

  @Test
  void whenBankReturns503ThenApiExceptionIsThrown() {
    PostPaymentRequest request = validRequest();

    when(restTemplate.exchange(
        eq("http://localhost:8080/payments"),
        eq(HttpMethod.POST),
        org.mockito.ArgumentMatchers.any(HttpEntity.class),
        eq(BankPaymentResponse.class)
    )).thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    ApiException ex = assertThrows(ApiException.class, () -> client.submitPayment(request));
    assertEquals(ApiErrorSpec.BANK_UNAVAILABLE, ex.getSpec());
  }

  @Test
  void whenSubmittingPaymentThenExpiryDateIsZeroPadded() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(4);
    request.setExpiryYear(2027);

    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(true);

    when(restTemplate.exchange(
        eq("http://localhost:8080/payments"),
        eq(HttpMethod.POST),
        org.mockito.ArgumentMatchers.any(HttpEntity.class),
        eq(BankPaymentResponse.class)
    )).thenReturn(ResponseEntity.ok(bankResponse));

    client.submitPayment(request);

    ArgumentCaptor<HttpEntity<BankPaymentRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).exchange(
        eq("http://localhost:8080/payments"),
        eq(HttpMethod.POST),
        captor.capture(),
        eq(BankPaymentResponse.class)
    );

    BankPaymentRequest bankRequest = captor.getValue().getBody();
    assertEquals("04/2027", bankRequest.getExpiryDate());
    assertEquals("4111111111111111", bankRequest.getCardNumber());
    assertEquals("USD", bankRequest.getCurrency());
    assertEquals(100, bankRequest.getAmount());
    assertEquals("123", bankRequest.getCvv());
  }

  @Test
  void toBankRequestFormatsExpiryDateCorrectly() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(12);
    request.setExpiryYear(2028);

    BankPaymentRequest bankRequest = DefaultAcquiringBankClient.toBankRequest(request);

    assertEquals("12/2028", bankRequest.getExpiryDate());
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
}
