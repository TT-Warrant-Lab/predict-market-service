package com.fadx.predict_market_service.core.datasource;

import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import com.fadx.predict_market_service.modules.common.services.ICallApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

/**
 * Ordered provider fallback from predict-core's datasource registry pattern. Add another {@link
 * MarketDataAdapter} bean to introduce a provider without changing controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FallbackMarketDataService implements ICallApiService {
  private final List<MarketDataAdapter> adapters;

  @Override
  public @Nullable <R> R callApi(
      HttpMethod method,
      EndpointType endpointType,
      Object body,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams,
      TypeReference<R> typeReference) {
    List<String> failures = new ArrayList<>();
    for (MarketDataAdapter adapter : adapters) {
      try {
        return adapter.call(method, endpointType, body, path, headers, queryParams, typeReference);
      } catch (RuntimeException error) {
        failures.add(adapter.providerName() + ": " + error.getMessage());
        log.warn(
            "Market provider {} failed for {} {}; trying fallback",
            adapter.providerName(),
            endpointType,
            path,
            error);
      }
    }
    throw new IllegalStateException("All market providers failed: " + String.join("; ", failures));
  }

  @Override
  public <R> R callApiGet(
      EndpointType endpointType,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams,
      TypeReference<R> typeReference) {
    return callApi(HttpMethod.GET, endpointType, null, path, headers, queryParams, typeReference);
  }

  @Override
  public List<Map<String, Object>> callApiGetReturnList(
      EndpointType endpointType,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams) {
    return callApi(
        HttpMethod.GET, endpointType, null, path, headers, queryParams, new TypeReference<>() {});
  }
}
