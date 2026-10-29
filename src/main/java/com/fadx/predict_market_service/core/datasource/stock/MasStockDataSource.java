package com.fadx.predict_market_service.core.datasource.stock;

import com.fadx.predict_market_service.modules.market.api.dtos.stock.ForeignHistoryRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyEventRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyNewsRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyOverviewRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyProfileRecord;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Direct MAS stock adapter. Provider query syntax and response conversion are isolated here. */
@Component
public class MasStockDataSource implements StockDataSource {
  private final RestClient client;
  private final ObjectMapper mapper;
  private final String endpoint;

  public MasStockDataSource(
      RestClient.Builder builder,
      ObjectMapper mapper,
      @Value("${app.mas-api.base-url}") String baseUrl,
      @Value("${app.mas-api.stock-endpoint}") String endpoint) {
    this.client = builder.baseUrl(baseUrl).build();
    this.mapper = mapper;
    this.endpoint = endpoint;
  }

  public List<ForeignHistoryRecord> foreignHistory(
      String symbol, String from, String to, String last, int limit) {
    return graph(
        "/foreignHistory",
        "query{vsForeignHistory(StockCode:\""
            + symbol
            + "\",FromDate:\""
            + from
            + "\",ToDate:\""
            + to
            + "\",lastTradingDate:\""
            + last
            + "\",fetchCount:"
            + limit
            + "){TotalBuyVol,TotalBuyVal,TotalSellVol,TotalSellVal,OwnedRatio,TradingDate,BuyVol,SellVol,BuyVal,SellVal}}",
        new TypeReference<>() {});
  }

  public CompanyOverviewRecord overview(String symbol) {
    return graph(
        "/companyInfo",
        "query{vsCompanyInfo(CompanyCode:\""
            + symbol
            + "\",LanguageID:1){FullName,Address,Phone,Fax,Email,URL,IndustryName,SectorName}}",
        new TypeReference<>() {});
  }

  public CompanyProfileRecord profile(String symbol) {
    return graph(
        "/stockInfo",
        "query{vsStockInfo(StockCode:\""
            + symbol
            + "\"){Max52W,Min52W,PerChange52W,MarketCapital,EPS,PE,PB,Beta,Dividend,Yield,KLCPLH,KLCPNY,StockStatus,TotalVol}}",
        new TypeReference<>() {});
  }

  public List<CompanyEventRecord> events(String symbol, int limit, int language) {
    return graph(
        "/stockEvent",
        "query{vsStockEventList(Code:\""
            + symbol
            + "\",fetchCount:"
            + limit
            + ",LanguageID:"
            + language
            + "){EventID,DateOrder,GDKHQDate,Code,Title,EventTypeID,Content}}",
        new TypeReference<>() {});
  }

  public List<CompanyNewsRecord> news(String symbol, int limit, int language) {
    return graph(
        "/news",
        "query{vsNewsList(LanguageID:"
            + language
            + ",StockCode:\""
            + symbol
            + "\",fetchCount:"
            + limit
            + "){ArticleID,Title,PublishTime}}",
        new TypeReference<>() {});
  }

  private <T> T graph(String path, String query, TypeReference<T> type) {
    var uri =
        UriComponentsBuilder.fromPath(endpoint + path)
            .queryParam("query", query)
            .build()
            .encode()
            .toUri();
    String body = client.get().uri(uri).retrieve().body(String.class);
    return mapper.readValue(body, mapper.getTypeFactory().constructType(type.getType()));
  }
}
