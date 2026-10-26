package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import tools.jackson.core.type.TypeReference;

public interface ICallApiService {

  @Nullable <R> R callApi(
      HttpMethod method,
      EndpointType endpointType,
      Object body,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams,
      TypeReference<R> typeReference);

  <R> R callApiGet(
      EndpointType endpointType,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams,
      TypeReference<R> typeReference);

  List<Map<String, Object>> callApiGetReturnList(
      EndpointType endpointType,
      String path,
      Map<String, String> headers,
      Map<String, String> queryParams);
}
