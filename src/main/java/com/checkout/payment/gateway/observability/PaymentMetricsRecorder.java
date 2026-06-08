package com.checkout.payment.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetricsRecorder {

  static final String AUTHORIZED = "payments.authorized";
  static final String DECLINED = "payments.declined";
  static final String FAILED = "payments.failed";
  static final String REASON_TAG = "reason";

  private final MeterRegistry meterRegistry;

  public PaymentMetricsRecorder(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordAuthorized() {
    meterRegistry.counter(AUTHORIZED).increment();
  }

  public void recordDeclined() {
    meterRegistry.counter(DECLINED).increment();
  }

  public void recordFailure(String reason) {
    meterRegistry.counter(FAILED, REASON_TAG, reason).increment();
  }

  public double getAuthorizedCount() {
    return counterCount(AUTHORIZED);
  }

  public double getDeclinedCount() {
    return counterCount(DECLINED);
  }

  public Map<String, Double> getFailuresByReason() {
    Map<String, Double> breakdown = new LinkedHashMap<>();
    for (Meter meter : meterRegistry.find(FAILED).counters()) {
      String reason = meter.getId().getTag(REASON_TAG);
      if (reason != null) {
        breakdown.merge(reason, ((Counter) meter).count(), Double::sum);
      }
    }
    return breakdown;
  }

  private double counterCount(String name) {
    Counter counter = meterRegistry.find(name).counter();
    return counter != null ? counter.count() : 0.0;
  }
}
