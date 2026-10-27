package com.fadx.predict_market_service.core.datasource.market;

import com.fadx.predict_market_service.modules.market.api.dtos.market.MatchedOrderPageRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.QuoteSummaryItemRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolCandleRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolQuoteRecord;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** MAS implementation of the market domain; all upstream URI knowledge stays here. */
@Component
public class MasMarketDataSource implements MarketDataSource {
  private final RestClient client;
  private final ObjectMapper mapper;
  private final String marketEndpoint;

  public MasMarketDataSource(
      RestClient.Builder builder,
      ObjectMapper mapper,
      @Value("${app.mas-api.base-url:https://masboard.masvn.com}") String baseUrl,
      @Value("${app.mas-api.market-endpoint:/api/v1/market}") String marketEndpoint) {
    this.client = builder.baseUrl(baseUrl).build();
    this.mapper = mapper;
    this.marketEndpoint = marketEndpoint;
  }

  @Override
  public List<SymbolQuoteRecord> currentPrices(List<String> symbols) {
    return get(
        "/symbolLatest",
        java.util.Map.of("symbolList", String.join(",", symbols)),
        new TypeReference<>() {});
  }

  @Override
  public MatchedOrderPageRecord quote(
      String symbol, int fetchCount, Integer lastIndex, Integer lastSize) {
    var query = new java.util.LinkedHashMap<String, String>();
    query.put("symbol", symbol);
    query.put("fetchCount", String.valueOf(fetchCount));
    if (lastIndex != null) query.put("lastIndex", String.valueOf(lastIndex));
    if (lastSize != null) query.put("lastSize", String.valueOf(lastSize));
    return get("/" + symbol + "/quote", query, new TypeReference<>() {});
  }

  @Override
  public List<QuoteSummaryItemRecord> quoteSummary(String symbol) {
    return get("/quoteSummary", java.util.Map.of("symbol", symbol), new TypeReference<>() {});
  }

  @Override
  public List<SymbolCandleRecord> history(
      String symbol, String fromDate, String toDate, int limit) {
    return get(
        "/symbolHistory",
        java.util.Map.of(
            "symbol", symbol, "from", fromDate, "to", toDate, "fetchCount", String.valueOf(limit)),
        new TypeReference<>() {});
  }

  private <T> T get(String path, java.util.Map<String, String> query, TypeReference<T> type) {
    UriComponentsBuilder uri = UriComponentsBuilder.fromPath(marketEndpoint + path);
    query.forEach(uri::queryParam);
    String body = client.get().uri(uri.build().encode().toUri()).retrieve().body(String.class);
    if (body == null) throw new IllegalStateException("MAS returned an empty response");
    return mapper.readValue(body, mapper.getTypeFactory().constructType(type.getType()));
  }
}
