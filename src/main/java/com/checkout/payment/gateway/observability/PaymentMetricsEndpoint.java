package com.checkout.payment.gateway.observability;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "payments")
public class PaymentMetricsEndpoint {

  private final PaymentMetricsRecorder metricsRecorder;

  public PaymentMetricsEndpoint(PaymentMetricsRecorder metricsRecorder) {
    this.metricsRecorder = metricsRecorder;
  }

  @ReadOperation
  public PaymentMetricsSnapshot payments() {
    Map<String, Long> failuresByReason = metricsRecorder.getFailuresByReason().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));
    long failureTotal = failuresByReason.values().stream().mapToLong(Long::longValue).sum();

    return new PaymentMetricsSnapshot(
        (long) metricsRecorder.getAuthorizedCount(),
        (long) metricsRecorder.getDeclinedCount(),
        failureTotal,
        failuresByReason);
  }
}
