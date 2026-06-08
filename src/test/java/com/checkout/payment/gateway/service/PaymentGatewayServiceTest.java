package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.observability.PaymentMetricsRecorder;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;
  @Mock
  private AcquiringBankClient acquiringBankClient;
  @Mock
  private PaymentMetricsRecorder paymentMetricsRecorder;

  private PaymentGatewayService service;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, acquiringBankClient,
        paymentMetricsRecorder);
  }

  @Test
  void whenPaymentIsAuthorizedThenResponseIsSavedAndReturned() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(request)).thenReturn(PaymentStatus.AUTHORIZED);

    PostPaymentResponse response = service.processPayment(request);

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals(1111, response.getCardNumberLastFour());
    assertEquals("USD", response.getCurrency());
    assertEquals(100, response.getAmount());
    verify(paymentsRepository).add(response);
    verify(paymentMetricsRecorder).recordAuthorized();
  }

  @Test
  void whenPaymentIsDeclinedThenResponseIsSavedAndReturned() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(request)).thenReturn(PaymentStatus.DECLINED);

    PostPaymentResponse response = service.processPayment(request);

    assertEquals(PaymentStatus.DECLINED, response.getStatus());
    verify(paymentsRepository).add(response);
    verify(paymentMetricsRecorder).recordDeclined();
  }

  @Test
  void whenBankIsUnavailableThenNothingIsSaved() {
    PostPaymentRequest request = validRequest();
    when(acquiringBankClient.submitPayment(request))
        .thenThrow(new ApiException(ApiErrorSpec.BANK_UNAVAILABLE,
            "Acquiring bank is currently unavailable"));

    assertThrows(ApiException.class, () -> service.processPayment(request));

    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenPaymentExistsThenItIsReturned() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse stored = new PostPaymentResponse();
    stored.setId(id);
    when(paymentsRepository.get(id)).thenReturn(Optional.of(stored));

    PostPaymentResponse result = service.getPaymentById(id);

    assertEquals(id, result.getId());
  }

  @Test
  void whenPaymentDoesNotExistThenExceptionIsThrown() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    ApiException ex = assertThrows(ApiException.class, () -> service.getPaymentById(id));
    assertEquals(ApiErrorSpec.NOT_FOUND, ex.getSpec());
    assertEquals("Payment with id " + id + " not found", ex.getMessage());
  }

  @Test
  void whenProcessingPaymentThenLastFourDigitsAreExtracted() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4111111111114321");
    when(acquiringBankClient.submitPayment(request)).thenReturn(PaymentStatus.AUTHORIZED);

    PostPaymentResponse response = service.processPayment(request);

    assertEquals(4321, response.getCardNumberLastFour());

    ArgumentCaptor<PostPaymentResponse> captor = ArgumentCaptor.forClass(PostPaymentResponse.class);
    verify(paymentsRepository).add(captor.capture());
    assertEquals(4321, captor.getValue().getCardNumberLastFour());
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
