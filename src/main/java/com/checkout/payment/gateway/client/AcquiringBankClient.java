package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;

public interface AcquiringBankClient {

  PaymentStatus submitPayment(PostPaymentRequest request);
}
