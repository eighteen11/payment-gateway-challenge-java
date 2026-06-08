package com.checkout.payment.gateway.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

  @Bean
  public GroupedOpenApi v1Api() {
    return GroupedOpenApi.builder()
        .group("v1")
        .pathsToMatch("/v1/**")
        .build();
  }

  @Bean
  public OpenAPI paymentGatewayOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Payment Gateway API")
            .description("Process card payments via an acquiring bank simulator")
            .version("1.0.0"));
  }
}
