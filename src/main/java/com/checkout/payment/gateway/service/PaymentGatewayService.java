package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.observability.PaymentMetricsRecorder;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;
  private final PaymentMetricsRecorder paymentMetricsRecorder;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      AcquiringBankClient acquiringBankClient,
      PaymentMetricsRecorder paymentMetricsRecorder) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBankClient = acquiringBankClient;
    this.paymentMetricsRecorder = paymentMetricsRecorder;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new ApiException(
            ApiErrorSpec.NOT_FOUND,
            "Payment with id %s not found".formatted(id)));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    long startTime = System.currentTimeMillis();
    PaymentStatus status = acquiringBankClient.submitPayment(paymentRequest);

    PostPaymentResponse response = buildResponse(paymentRequest, status);
    paymentsRepository.add(response);
    recordPaymentOutcome(status);

    LOG.info("Payment processed: paymentId={}, status={}, durationMs={}",
        response.getId(), response.getStatus().getName(),
        System.currentTimeMillis() - startTime);

    return response;
  }

  private void recordPaymentOutcome(PaymentStatus status) {
    if (status == PaymentStatus.AUTHORIZED) {
      paymentMetricsRecorder.recordAuthorized();
    } else if (status == PaymentStatus.DECLINED) {
      paymentMetricsRecorder.recordDeclined();
    }
  }

  private PostPaymentResponse buildResponse(PostPaymentRequest request, PaymentStatus status) {
    String cardNumber = request.getCardNumber();
    int lastFour = Integer.parseInt(cardNumber.substring(cardNumber.length() - 4));

    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(status);
    response.setCardNumberLastFour(lastFour);
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency());
    response.setAmount(request.getAmount());
    return response;
  }
}
