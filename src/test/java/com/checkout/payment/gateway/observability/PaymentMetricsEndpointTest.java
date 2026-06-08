package com.checkout.payment.gateway.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentMetricsEndpointTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private PaymentsRepository paymentsRepository;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @BeforeEach
  void setUp() {
    paymentsRepository.clear();
  }

  @Test
  void whenPaymentsAreProcessedThenActuatorEndpointReflectsCounts() throws Exception {
    JsonNode baseline = fetchMetrics();

    when(acquiringBankClient.submitPayment(any()))
        .thenReturn(PaymentStatus.AUTHORIZED)
        .thenReturn(PaymentStatus.DECLINED)
        .thenThrow(new ApiException(ApiErrorSpec.BANK_UNAVAILABLE,
            "Acquiring bank is currently unavailable"));

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111111"))))
        .andExpect(status().isCreated());

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111112"))))
        .andExpect(status().isCreated());

    Map<String, Object> invalidBody = validPaymentBody("4111111111111111");
    invalidBody.put("amount", 0);
    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidBody)))
        .andExpect(status().isBadRequest());

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111110"))))
        .andExpect(status().isBadGateway());

    JsonNode metrics = fetchMetrics();
    assertEquals(baseline.get("successful").asLong() + 1, metrics.get("successful").asLong());
    assertEquals(baseline.get("declined").asLong() + 1, metrics.get("declined").asLong());
    assertEquals(baseline.get("failureTotal").asLong() + 2, metrics.get("failureTotal").asLong());
    assertEquals(
        countForReason(baseline, "VALIDATION_ERROR") + 1,
        countForReason(metrics, "VALIDATION_ERROR"));
    assertEquals(
        countForReason(baseline, "BANK_UNAVAILABLE") + 1,
        countForReason(metrics, "BANK_UNAVAILABLE"));
  }

  private JsonNode fetchMetrics() throws Exception {
    String response = mvc.perform(MockMvcRequestBuilders.get("/actuator/payments"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response);
  }

  private long countForReason(JsonNode metrics, String reason) {
    JsonNode failuresByReason = metrics.get("failuresByReason");
    return failuresByReason.has(reason) ? failuresByReason.get(reason).asLong() : 0L;
  }

  private Map<String, Object> validPaymentBody(String cardNumber) {
    YearMonth future = YearMonth.now().plusMonths(6);
    Map<String, Object> body = new HashMap<>();
    body.put("card_number", cardNumber);
    body.put("expiry_month", future.getMonthValue());
    body.put("expiry_year", future.getYear());
    body.put("currency", "USD");
    body.put("amount", 100);
    body.put("cvv", "123");
    return body;
  }
}
