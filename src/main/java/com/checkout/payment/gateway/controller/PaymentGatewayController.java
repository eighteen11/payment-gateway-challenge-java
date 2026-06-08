package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ApiErrorResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
@RequestMapping("/v1")
@Tag(name = "Payment Gateway v1", description = "Process and retrieve card payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;
  private final PaymentRequestValidator paymentRequestValidator;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService,
      PaymentRequestValidator paymentRequestValidator) {
    this.paymentGatewayService = paymentGatewayService;
    this.paymentRequestValidator = paymentRequestValidator;
  }

  @PostMapping("/payment")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Process a payment",
      description = "Validates the request, submits to the acquiring bank, and stores the result")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Payment processed",
          content = @Content(schema = @Schema(implementation = PostPaymentResponse.class))),
      @ApiResponse(responseCode = "400", description = "Validation failed",
          content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
      @ApiResponse(responseCode = "502", description = "Acquiring bank unavailable",
          content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public PostPaymentResponse processPayment(@Valid @RequestBody PostPaymentRequest request) {
    paymentRequestValidator.validate(request);
    return paymentGatewayService.processPayment(request);
  }

  @GetMapping("/payment/{id}")
  @Operation(summary = "Get a payment by ID",
      description = "Retrieves a previously processed authorized or declined payment")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found",
          content = @Content(schema = @Schema(implementation = PostPaymentResponse.class))),
      @ApiResponse(responseCode = "404", description = "Payment not found",
          content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public PostPaymentResponse getPostPaymentEventById(@PathVariable UUID id) {
    return paymentGatewayService.getPaymentById(id);
  }
}
