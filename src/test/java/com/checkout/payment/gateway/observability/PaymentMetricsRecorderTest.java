package com.checkout.payment.gateway.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentMetricsRecorderTest {

  private PaymentMetricsRecorder recorder;

  @BeforeEach
  void setUp() {
    recorder = new PaymentMetricsRecorder(new SimpleMeterRegistry());
  }

  @Test
  void whenPaymentsAreRecordedThenCountsAreAvailable() {
    recorder.recordAuthorized();
    recorder.recordAuthorized();
    recorder.recordDeclined();
    recorder.recordFailure("VALIDATION_ERROR");
    recorder.recordFailure("BANK_UNAVAILABLE");
    recorder.recordFailure("VALIDATION_ERROR");

    assertEquals(2, recorder.getAuthorizedCount());
    assertEquals(1, recorder.getDeclinedCount());
    assertEquals(2, recorder.getFailuresByReason().get("VALIDATION_ERROR"));
    assertEquals(1, recorder.getFailuresByReason().get("BANK_UNAVAILABLE"));
  }
}
