package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import com.fadx.predict_market_service.modules.common.services.ICallApiService;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.ForeignHistoryRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyEventRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyNewsRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyOverviewRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyProfileRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Anonymous | Stock", description = "API for Stock data")
public class StockController {

  private final ICallApiService callApiService;

  /** GET /api/stock/{symbol}/foreign-history Get foreign buy/sell flow history */
  @GetMapping("/{symbol}/foreign-history")
  public List<ForeignHistoryRecord> getForeignHistory(
      @PathVariable String symbol,
      @RequestParam String from_date,
      @RequestParam String to_date,
      @RequestParam String last_trading_date,
      @RequestParam(defaultValue = "100") Integer limit) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("from_date", from_date);
    queryParams.put("to_date", to_date);
    queryParams.put("last_trading_date", last_trading_date);
    queryParams.put("limit", limit.toString());

    String path = "/" + symbol + "/foreign-history";

    return callApiService.callApiGet(
        EndpointType.STOCK,
        path,
        null,
        queryParams,
        new TypeReference<List<ForeignHistoryRecord>>() {});
  }

  /**
   * GET /api/stock/{symbol}/overview Get company overview and ownership structure No query params.
   */
  @GetMapping("/{symbol}/overview")
  public CompanyOverviewRecord getCompanyOverview(@PathVariable String symbol) {

    String path = "/" + symbol + "/overview";

    return callApiService.callApiGet(
        EndpointType.STOCK, path, null, null, new TypeReference<CompanyOverviewRecord>() {});
  }

  /**
   * GET /api/stock/{symbol}/profile Get company financial profile and market metrics No query
   * params.
   */
  @GetMapping("/{symbol}/profile")
  public CompanyProfileRecord getCompanyProfile(@PathVariable String symbol) {

    String path = "/" + symbol + "/profile";

    return callApiService.callApiGet(
        EndpointType.STOCK, path, null, null, new TypeReference<CompanyProfileRecord>() {});
  }

  /** GET /api/stock/{symbol}/events Get corporate event calendar */
  @GetMapping("/{symbol}/events")
  public List<CompanyEventRecord> getCompanyEvents(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "100") Integer limit,
      @RequestParam(defaultValue = "1") Integer language_id) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("limit", limit.toString());
    queryParams.put("language_id", language_id.toString());

    String path = "/" + symbol + "/events";

    return callApiService.callApiGet(
        EndpointType.STOCK,
        path,
        null,
        queryParams,
        new TypeReference<List<CompanyEventRecord>>() {});
  }

  /** GET /api/stock/{symbol}/news Get latest news articles for a symbol */
  @GetMapping("/{symbol}/news")
  public List<CompanyNewsRecord> getCompanyNews(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "50") Integer limit,
      @RequestParam(defaultValue = "1") Integer language_id) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("limit", limit.toString());
    queryParams.put("language_id", language_id.toString());

    String path = "/" + symbol + "/news";

    return callApiService.callApiGet(
        EndpointType.STOCK,
        path,
        null,
        queryParams,
        new TypeReference<List<CompanyNewsRecord>>() {});
  }
}
