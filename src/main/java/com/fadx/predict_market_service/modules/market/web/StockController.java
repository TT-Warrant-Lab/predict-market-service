package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.core.datasource.stock.StockDataSource;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.ForeignHistoryRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyEventRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyNewsRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyOverviewRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyProfileRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Anonymous | Stock", description = "API for Stock data")
public class StockController {

  private final StockDataSource stockDataSource;

  /** GET /api/stock/{symbol}/foreign-history Get foreign buy/sell flow history */
  @GetMapping("/{symbol}/foreign-history")
  public List<ForeignHistoryRecord> getForeignHistory(
      @PathVariable String symbol,
      @RequestParam String from_date,
      @RequestParam String to_date,
      @RequestParam String last_trading_date,
      @RequestParam(defaultValue = "100") Integer limit) {

    return stockDataSource.foreignHistory(symbol, from_date, to_date, last_trading_date, limit);
  }

  /**
   * GET /api/stock/{symbol}/overview Get company overview and ownership structure No query params.
   */
  @GetMapping("/{symbol}/overview")
  public CompanyOverviewRecord getCompanyOverview(@PathVariable String symbol) {

    return stockDataSource.overview(symbol);
  }

  /**
   * GET /api/stock/{symbol}/profile Get company financial profile and market metrics No query
   * params.
   */
  @GetMapping("/{symbol}/profile")
  public CompanyProfileRecord getCompanyProfile(@PathVariable String symbol) {

    return stockDataSource.profile(symbol);
  }

  /** GET /api/stock/{symbol}/events Get corporate event calendar */
  @GetMapping("/{symbol}/events")
  public List<CompanyEventRecord> getCompanyEvents(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "100") Integer limit,
      @RequestParam(defaultValue = "1") Integer language_id) {

    return stockDataSource.events(symbol, limit, language_id);
  }

  /** GET /api/stock/{symbol}/news Get latest news articles for a symbol */
  @GetMapping("/{symbol}/news")
  public List<CompanyNewsRecord> getCompanyNews(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "50") Integer limit,
      @RequestParam(defaultValue = "1") Integer language_id) {

    return stockDataSource.news(symbol, limit, language_id);
  }
}
