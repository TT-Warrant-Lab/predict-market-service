package com.fadx.predict_market_service.core.datasource;

import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import tools.jackson.core.type.TypeReference;

/** Provider boundary: controllers depend on this contract, never a vendor client. */
public interface MarketDataAdapter {
  String providerName();

  @Nullable <R> R call(
      HttpMethod method,
      EndpointType endpointType,
      Object body,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams,
      TypeReference<R> typeReference);

  default List<Map<String, Object>> list(
      EndpointType endpointType,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams) {
    return call(
        HttpMethod.GET, endpointType, null, path, headers, queryParams, new TypeReference<>() {});
  }
}
