package com.fadx.predict_market_service.core.datasource.trading;

import com.fadx.predict_market_service.modules.market.api.dtos.trading.DividendRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class MasTradingDataSource implements TradingDataSource {
  private final RestClient client;
  private final ObjectMapper mapper;
  private final String endpoint;

  public MasTradingDataSource(
      RestClient.Builder builder,
      ObjectMapper mapper,
      @Value("${app.mas-api.base-url}") String baseUrl,
      @Value("${app.mas-api.trading-endpoint}") String endpoint) {
    this.client = builder.baseUrl(baseUrl).build();
    this.mapper = mapper;
    this.endpoint = endpoint;
  }

  @Override
  public OHLCVResponse historical(String symbol, String fromDate, String toDate, String interval) {
    return get(
        "/history",
        java.util.Map.of(
            "symbol",
            symbol,
            "from",
            fromDate,
            "to",
            toDate,
            "resolution",
            interval == null ? "1D" : interval),
        new TypeReference<>() {});
  }

  @Override
  public List<DividendRecord> dividends(String symbol, String fromDate, String toDate) {
    return get(
        "/dividend",
        java.util.Map.of("symbol", symbol, "fromDate", fromDate, "toDate", toDate, "lang", "vi_VN"),
        new TypeReference<>() {});
  }

  private <T> T get(String path, java.util.Map<String, String> query, TypeReference<T> type) {
    var uri = UriComponentsBuilder.fromPath(endpoint + path);
    query.forEach(uri::queryParam);
    String body = client.get().uri(uri.build().encode().toUri()).retrieve().body(String.class);
    return mapper.readValue(body, mapper.getTypeFactory().constructType(type.getType()));
  }
}
