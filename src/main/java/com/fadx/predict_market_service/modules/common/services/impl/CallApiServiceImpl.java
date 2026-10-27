package com.fadx.predict_market_service.modules.common.services.impl;

import com.fadx.predict_market_service.core.datasource.MarketDataAdapter;
import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class CallApiServiceImpl implements MarketDataAdapter {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public CallApiServiceImpl(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      @Value("${app.predict-core.base-url}") String baseUrl) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.objectMapper = objectMapper;
  }

  @Override
  public String providerName() {
    return "predict-core-mas";
  }

  @Override
  public @Nullable <R> R call(
      HttpMethod method,
      EndpointType endpointType,
      Object body,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams,
      TypeReference<R> typeReference) {
    UriComponentsBuilder uri =
        UriComponentsBuilder.fromPath("/api/")
            .path(endpointType.getValue())
            .path(path == null ? "" : path);
    if (queryParams != null) {
      queryParams.forEach(uri::queryParam);
    }

    RestClient.RequestBodySpec request =
        restClient.method(method).uri(uri.build().encode().toUri());
    if (headers != null) {
      headers.forEach(request::header);
    }
    RestClient.RequestHeadersSpec<?> outgoing = body == null ? request : request.body(body);
    String response = outgoing.retrieve().body(String.class);
    if (response == null) {
      return null;
    }
    JavaType type = objectMapper.getTypeFactory().constructType(typeReference.getType());
    return objectMapper.readValue(response, type);
  }
}
