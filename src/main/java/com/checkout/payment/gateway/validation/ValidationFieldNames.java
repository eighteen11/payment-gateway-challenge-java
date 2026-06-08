package com.checkout.payment.gateway.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class ValidationFieldNames {

  private static final Map<String, String> JAVA_TO_JSON = buildFieldNameMap();

  private ValidationFieldNames() {
  }

  public static String toJsonFieldName(String javaFieldName) {
    if (javaFieldName == null || javaFieldName.isBlank()) {
      return javaFieldName;
    }
    return JAVA_TO_JSON.getOrDefault(javaFieldName, javaFieldName);
  }

  private static Map<String, String> buildFieldNameMap() {
    Map<String, String> map = new HashMap<>();
    for (Field field : com.checkout.payment.gateway.model.PostPaymentRequest.class
        .getDeclaredFields()) {
      JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
      if (jsonProperty != null) {
        map.put(field.getName(), jsonProperty.value());
      } else {
        map.put(field.getName(), field.getName());
      }
    }
    return map;
  }
}
