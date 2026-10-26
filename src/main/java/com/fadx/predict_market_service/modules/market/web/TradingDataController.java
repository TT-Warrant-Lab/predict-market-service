package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.modules.market.api.dtos.ForeignChartResponseDto;
import com.fadx.predict_market_service.modules.common.dto.TickerChartResponseDto;
import com.fadx.predict_market_service.modules.common.services.StockChartService;
import com.fadx.predict_market_service.modules.common.services.StockTradingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trading")
@Tag(name = "Anonymous | Trading Data", description = "API for managing anonymous trading data")
@RequiredArgsConstructor
public class TradingDataController {

  private final StockTradingService stockTradingService;
  private final StockChartService chartService;

  // 1. Chỉ lấy biểu đồ giá và khối lượng tổng
  @GetMapping("/trend/ticker/{ticker}")
  public ResponseEntity<Map<String, Object>> getTickerTrend(@PathVariable String ticker) {
    return ResponseEntity.ok(stockTradingService.getTickerTrend(ticker.trim().toUpperCase()));
  }

  // 2. Chỉ lấy biểu đồ dòng tiền khối ngoại ròng
  @GetMapping("/trend/foreign/{ticker}")
  public ResponseEntity<Map<String, Object>> getForeignTrend(@PathVariable String ticker) {
    return ResponseEntity.ok(stockTradingService.getForeignTrend(ticker.trim().toUpperCase()));
  }

  // 3. Lấy bảng điện snapshot mới nhất toàn bộ dữ liệu gốc
  @GetMapping("/latest")
  public ResponseEntity<Map<String, Object>> getLatestPrices(
      @RequestParam("tickers") String tickersParam) {
    if (tickersParam == null || tickersParam.trim().isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    List<String> tickerList =
        Arrays.stream(tickersParam.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .filter(s -> !s.isEmpty())
            .toList();

    return ResponseEntity.ok(stockTradingService.getLatestByList(tickerList));
  }

  /**
   * Chart giá + volume theo phút (1m) trong 1 ngày giao dịch của ticker. GET
   * /api/v1/tickers/{ticker}/chart?date=2026-07-16
   */
  @GetMapping("/{ticker}/chart")
  public ResponseEntity<TickerChartResponseDto> getTickerChart(
      @PathVariable String ticker,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(chartService.getIntradayTickerChart(ticker.toUpperCase(), date));
  }

  /**
   * Chart khối lượng mua/bán/net khối ngoại theo phút (1m) trong 1 ngày giao dịch. GET
   * /api/v1/tickers/{ticker}/foreign-chart?date=2026-07-16
   */
  @GetMapping("/{ticker}/foreign-chart")
  public ResponseEntity<ForeignChartResponseDto> getForeignChart(
      @PathVariable String ticker,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(chartService.getIntradayForeignChart(ticker.toUpperCase(), date));
  }
}
