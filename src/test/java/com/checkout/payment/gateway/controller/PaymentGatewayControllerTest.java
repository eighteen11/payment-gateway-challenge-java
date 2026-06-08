package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.ApiErrorSpec;
import com.checkout.payment.gateway.exception.ApiException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private PaymentsRepository paymentsRepository;
  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @BeforeEach
  void setUp() {
    paymentsRepository.clear();
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/v1/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    UUID missingId = UUID.randomUUID();
    mvc.perform(MockMvcRequestBuilders.get("/v1/payment/" + missingId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Payment with id " + missingId + " not found"));
  }

  @Test
  void whenPaymentIsAuthorizedThen201IsReturned() throws Exception {
    when(acquiringBankClient.submitPayment(any())).thenReturn(PaymentStatus.AUTHORIZED);

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111111"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(1111))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.currency").value("USD"));
  }

  @Test
  void whenPaymentIsDeclinedThen201IsReturned() throws Exception {
    when(acquiringBankClient.submitPayment(any())).thenReturn(PaymentStatus.DECLINED);

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111112"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void whenRequestBodyIsEmptyThen400IsReturnedWithRequiredFieldErrors() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[?(@.field == 'card_number')].message")
            .value("Card number is required"))
        .andExpect(jsonPath("$.errors[?(@.field == 'expiry_month')].message")
            .value("Expiry month is required"))
        .andExpect(jsonPath("$.errors[?(@.field == 'expiry_year')].message")
            .value("Expiry year is required"))
        .andExpect(jsonPath("$.errors[?(@.field == 'currency')].message")
            .value("Currency is required"))
        .andExpect(jsonPath("$.errors[?(@.field == 'amount')].message")
            .value("Amount is required"))
        .andExpect(jsonPath("$.errors[?(@.field == 'cvv')].message")
            .value("CVV is required"));

    verify(acquiringBankClient, never()).submitPayment(any());
  }

  @Test
  void whenExpiryDateIsInThePastThen400IsReturnedAndBankIsNotCalled() throws Exception {
    Map<String, Object> body = validPaymentBody("4111111111111111");
    YearMonth past = YearMonth.now().minusMonths(1);
    body.put("expiry_month", past.getMonthValue());
    body.put("expiry_year", past.getYear());

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[?(@.field == 'expiry_date')].message")
            .value("Card expiry date must be in the future"));

    verify(acquiringBankClient, never()).submitPayment(any());
  }

  @Test
  void whenPaymentIsRejectedThen400IsReturnedAndBankIsNotCalled() throws Exception {
    Map<String, Object> body = validPaymentBody("4111111111111111");
    body.put("amount", 0);

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors").isArray());

    verify(acquiringBankClient, never()).submitPayment(any());
  }

  @Test
  void whenBankIsUnavailableThen502IsReturned() throws Exception {
    when(acquiringBankClient.submitPayment(any()))
        .thenThrow(new ApiException(ApiErrorSpec.BANK_UNAVAILABLE,
            "Acquiring bank is currently unavailable"));

    mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111110"))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("BANK_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Acquiring bank is currently unavailable"));
  }

  @Test
  void whenPaymentIsPostedThenItCanBeRetrievedById() throws Exception {
    when(acquiringBankClient.submitPayment(any())).thenReturn(PaymentStatus.AUTHORIZED);

    String response = mvc.perform(MockMvcRequestBuilders.post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPaymentBody("4111111111111111"))))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String paymentId = objectMapper.readTree(response).get("id").asText();

    mvc.perform(MockMvcRequestBuilders.get("/v1/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value("Authorized"));
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
