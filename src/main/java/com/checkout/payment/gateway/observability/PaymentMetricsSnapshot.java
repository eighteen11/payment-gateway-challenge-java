package com.checkout.payment.gateway.observability;

import java.util.Map;

public record PaymentMetricsSnapshot(
    long successful,
    long declined,
    long failureTotal,
    Map<String, Long> failuresByReason
) {}
