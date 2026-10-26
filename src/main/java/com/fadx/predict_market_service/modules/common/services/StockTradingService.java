package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.modules.common.entity.StockForeignTrendRaw;
import com.fadx.predict_market_service.modules.common.entity.StockMarketLatest;
import com.fadx.predict_market_service.modules.common.entity.StockTickerTrendRaw;
import com.fadx.predict_market_service.modules.common.repositories.StockForeignTrendRawRepository;
import com.fadx.predict_market_service.modules.common.repositories.StockMarketLatestRepository;
import com.fadx.predict_market_service.modules.common.repositories.StockTickerTrendRawRepository;
import com.fadx.predict_market_service.modules.market.internal.RedisService;
import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import com.fadx.predict_market_service.modules.symbol.internal.repositories.SymbolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTradingService {

  private final RedisService redisService;
  private final ObjectMapper objectMapper;

  private final StockMarketLatestRepository marketLatestRepository;
  private final StockTickerTrendRawRepository tickerTrendRawRepository;
  private final StockForeignTrendRawRepository foreignTrendRawRepository;
  private final IndexMembershipService indexMembershipService;
  private final SymbolRepository symbolRepository;

  // Định nghĩa Batch Size để cắt nhỏ mảng lớn khi lưu xuống DB
  private static final int BATCH_SIZE = 1000;

  // Khởi tạo Thread Pool cố định gồm 3 Thread phục vụ riêng cho tác vụ ETL cuối
  // ngày
  private final ExecutorService etlExecutor = Executors.newFixedThreadPool(3);

  /**
   * HÀM 1: Lấy chuỗi lịch sử biến động tổng (Nội + Ngoại) trong ngày Đọc từ:
   * ticker_trend:[Ngày]:[Mã] (ZSET) Dữ liệu bóc tách: Price|TotalVolume|MatchSide
   */
  public Map<String, Object> getTickerTrend(String ticker) {
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String zsetKey = "ticker_trend:" + currentDate + ":" + ticker;

    List<redis.clients.jedis.resps.Tuple> rawData =
        redisService.zrangeByScoreWithScores(
            zsetKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    List<Map<String, Object>> trendTimeline = new ArrayList<>();

    for (redis.clients.jedis.resps.Tuple tuple : rawData) {
      long timestamp = (long) tuple.getScore();
      String[] parts = tuple.getElement().split("\\|");

      if (parts.length < 3) continue;

      Map<String, Object> point = new HashMap<>();
      point.put("time", timestamp);
      point.put("price", Double.parseDouble(parts[0]));
      point.put("total_volume", Long.parseLong(parts[1]));
      point.put("match_side", parts[2]); // BUY / SELL chủ động
      // Các field mới - chỉ có ở dữ liệu ghi sau khi nâng cấp, giữ tương thích ngược
      if (parts.length >= 5) {
        point.put("net_volume", Long.parseLong(parts[3]));
        point.put("match_volume", Long.parseLong(parts[4]));
      }
      trendTimeline.add(point);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("ticker", ticker);
    response.put("type", "ticker_trend");
    response.put("data", trendTimeline);
    return response;
  }

  /**
   * HÀM 2: Lấy chuỗi lịch sử dòng tiền khối ngoại trong ngày Đọc từ: foreign_trend:[Ngày]:[Mã]
   * (ZSET) Dữ liệu bóc tách: foreignBuy|foreignSell|foreignNet
   */
  public Map<String, Object> getForeignTrend(String ticker) {
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String zsetKey = "foreign_trend:" + currentDate + ":" + ticker;

    List<redis.clients.jedis.resps.Tuple> rawData =
        redisService.zrangeByScoreWithScores(
            zsetKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    List<Map<String, Object>> foreignTimeline = new ArrayList<>();

    for (redis.clients.jedis.resps.Tuple tuple : rawData) {
      long timestamp = (long) tuple.getScore();
      String[] parts = tuple.getElement().split("\\|");

      if (parts.length < 3) continue;

      Map<String, Object> point = new HashMap<>();
      point.put("time", timestamp);
      point.put("foreign_buy", Long.parseLong(parts[0]));
      point.put("foreign_sell", Long.parseLong(parts[1]));
      point.put("foreign_net", Long.parseLong(parts[2])); // Ròng = Mua - Bán
      // Các field mới - chỉ có ở dữ liệu ghi sau khi nâng cấp, giữ tương thích ngược
      if (parts.length >= 5) {
        point.put("current_price", Double.parseDouble(parts[3]));
        point.put("ohlc", parts[4]); // chuỗi JSON {"o":..,"h":..,"l":..,"c":..}
      }
      foreignTimeline.add(point);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("ticker", ticker);
    response.put("type", "foreign_trend");
    response.put("data", foreignTimeline);
    return response;
  }

  /**
   * HÀM 3: Lấy toàn bộ dữ liệu gốc mới nhất của một danh sách mã chứng khoán Đọc từ:
   * market:latest:[Ngày] (HSET) Dữ liệu bóc tách: Parse ngược chuỗi JSON nguyên bản được lưu bởi
   * Kafka Pipeline
   */
  public Map<String, Object> getLatestByList(List<String> tickers) {
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String hashKey = "market:latest:" + currentDate;

    List<String> rawJsonValues = redisService.hmget(hashKey, tickers.toArray(new String[0]));
    Map<String, Object> finalResult = new HashMap<>();

    for (int i = 0; i < tickers.size(); i++) {
      String ticker = tickers.get(i);
      String rawVal = rawJsonValues.get(i);

      if (rawVal != null) {
        rawVal = rawVal.trim();
        // TRƯỜNG HỢP 1: Dữ liệu chuẩn định dạng JSON thô mới
        if (rawVal.startsWith("{")) {
          try {
            Map<String, Object> rawMap =
                objectMapper.readValue(rawVal, new TypeReference<Map<String, Object>>() {});
            finalResult.put(ticker, rawMap);
          } catch (Exception e) {
            log.error("[Redis-API] Lỗi giải mã JSON cho mã: {}", ticker, e);
            finalResult.put(ticker, null);
          }
        }
        // TRƯỜNG HỢP 2: Cứu vãn nếu trúng dữ liệu cũ dạng chuỗi
        // "Price|TotalVol|Side|..."
        else if (rawVal.contains("|")) {
          try {
            String[] parts = rawVal.split("\\|");
            Map<String, Object> fallbackMap = new HashMap<>();
            fallbackMap.put("s", ticker);
            fallbackMap.put("c", Double.parseDouble(parts[0]));
            fallbackMap.put("vo", Long.parseLong(parts[1]));
            fallbackMap.put("mb", parts[2]);

            // Nếu chuỗi cũ có đủ 5 trường khối ngoại
            if (parts.length >= 5) {
              fallbackMap.put("frBvo", Long.parseLong(parts[3]));
              fallbackMap.put("frSvo", Long.parseLong(parts[4]));
            }

            finalResult.put(ticker, fallbackMap);
            log.warn(
                "[Redis-API] Phát hiện và tự động chuyển đổi dữ liệu dạng chuỗi cũ cho mã: {}",
                ticker);
          } catch (Exception ex) {
            finalResult.put(ticker, null);
          }
        } else {
          finalResult.put(ticker, null);
        }
      } else {
        finalResult.put(ticker, null);
      }
    }
    return finalResult;
  }

  public void loadAllData() {
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    LocalDate tradingDate = LocalDate.now();
    log.info("[🚀 ETL-Parallel] Bắt đầu tiến trình ĐA LUỒNG đồng bộ dữ liệu ngày: {}", currentDate);

    // Bước 1: Quét nhanh danh sách mã từ Redis
    String pattern = "ticker_trend:" + currentDate + ":*";
    Set<String> tickerTrendKeys = redisService.keys(pattern);

    if (tickerTrendKeys == null || tickerTrendKeys.isEmpty()) {
      log.warn("[ETL-Parallel] Không tìm thấy dữ liệu trên Redis cho ngày hôm nay.");
      return;
    }

    List<String> activeTickers =
        tickerTrendKeys.stream().map(key -> key.substring(key.lastIndexOf(":") + 1)).toList();
    Map<String, String> marketsByTicker =
        symbolRepository.findAllById(activeTickers).stream()
            .collect(Collectors.toMap(Symbol::getSymbol, Symbol::getMarket));

    log.info(
        "[ETL-Parallel] Kích hoạt 3 Threads xử lý song song cho {} mã...", activeTickers.size());

    long startTime = System.currentTimeMillis();

    // ─── THREAD 1: XỬ LÝ LỊCH SỬ BIẾN ĐỘNG GIÁ (TICKER TREND)
    // ─────────────────────
    CompletableFuture<Void> tickerTrendTask =
        CompletableFuture.runAsync(
            () -> {
              log.info("[Thread-1] Đang cào và đóng gói Ticker Trend từ Redis...");
              List<StockTickerTrendRaw> entities = new ArrayList<>();

              for (String ticker : activeTickers) {
                String key = "ticker_trend:" + currentDate + ":" + ticker;
                List<redis.clients.jedis.resps.Tuple> rawData =
                    redisService.zrangeByScoreWithScores(
                        key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

                for (redis.clients.jedis.resps.Tuple tuple : rawData) {
                  String[] parts = tuple.getElement().split("\\|");
                  if (parts.length < 3) continue;

                  StockTickerTrendRaw.StockTickerTrendRawBuilder builder =
                      StockTickerTrendRaw.builder()
                          .ticker(ticker)
                          .tradingDate(tradingDate)
                          .tradingTime(convertTimestampToDateTime((long) tuple.getScore()))
                          .price(Double.parseDouble(parts[0]))
                          .accumulatedVolume(Long.parseLong(parts[1]))
                          .matchSide(parts[2]);

                  entities.add(builder.build());
                }
              }
              // Thực thi lưu xuống DB theo từng cụm Batch
              if (!entities.isEmpty()) {
                saveInBatches(entities, tickerTrendRawRepository::saveAll);
                log.info(
                    "[Thread-1] ✓ Đã lưu thành công {} dòng Ticker Trend bằng cơ chế Batch.",
                    entities.size());
              }
            },
            etlExecutor);

    // ─── THREAD 2: XỬ LÝ LỊCH SỬ DÒNG TIỀN NGOẠI (FOREIGN TREND)
    // ──────────────────
    CompletableFuture<Void> foreignTrendTask =
        CompletableFuture.runAsync(
            () -> {
              log.info("[Thread-2] Đang cào và đóng gói Foreign Trend từ Redis...");
              List<StockForeignTrendRaw> entities = new ArrayList<>();

              for (String ticker : activeTickers) {
                String key = "foreign_trend:" + currentDate + ":" + ticker;
                List<redis.clients.jedis.resps.Tuple> rawData =
                    redisService.zrangeByScoreWithScores(
                        key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

                for (redis.clients.jedis.resps.Tuple tuple : rawData) {
                  String[] parts = tuple.getElement().split("\\|");
                  if (parts.length < 3) continue;

                  StockForeignTrendRaw.StockForeignTrendRawBuilder builder =
                      StockForeignTrendRaw.builder()
                          .ticker(ticker)
                          .tradingDate(tradingDate)
                          .tradingTime(convertTimestampToDateTime((long) tuple.getScore()))
                          .foreignBuyVol(Long.parseLong(parts[0]))
                          .foreignSellVol(Long.parseLong(parts[1]))
                          .foreignNetVol(Long.parseLong(parts[2]));

                  // Dữ liệu Redis ghi trước khi nâng cấp sẽ không có 2 field này -> để null
                  if (parts.length >= 5) {
                    builder.currentPrice(Double.parseDouble(parts[3])).ohlc(parts[4]);
                  }

                  entities.add(builder.build());
                }
              }
              if (!entities.isEmpty()) {
                saveInBatches(entities, foreignTrendRawRepository::saveAll);
                log.info(
                    "[Thread-2] ✓ Đã lưu thành công {} dòng Foreign Trend bằng cơ chế Batch.",
                    entities.size());
              }
            },
            etlExecutor);

    // ─── THREAD 3: XỬ LÝ SNAPSHOT BẢNG ĐIỆN LATEST (MARKET LATEST)
    // ────────────────
    CompletableFuture<Void> marketLatestTask =
        CompletableFuture.runAsync(
            () -> {
              log.info("[Thread-3] Đang cào và đóng gói Market Latest từ Redis...");
              List<StockMarketLatest> entities = new ArrayList<>();
              String hashKey = "market:latest:" + currentDate;
              List<String> rawJsonValues =
                  redisService.hmget(hashKey, activeTickers.toArray(new String[0]));

              for (int i = 0; i < activeTickers.size(); i++) {
                String ticker = activeTickers.get(i);
                String jsonVal = rawJsonValues.get(i);
                if (jsonVal == null) continue;

                try {
                  Map<String, Object> root =
                      objectMapper.readValue(jsonVal, new TypeReference<Map<String, Object>>() {});
                  if (!root.containsKey("p")) continue;
                  List<Object> pList = (List<Object>) root.get("p");
                  if (pList == null || pList.size() < 2 || !(pList.get(1) instanceof Map)) continue;

                  Map<String, Object> data = (Map<String, Object>) pList.get(1);
                  long fBuy =
                      data.containsKey("frBvo") ? ((Number) data.get("frBvo")).longValue() : 0L;
                  long fSell =
                      data.containsKey("frSvo") ? ((Number) data.get("frSvo")).longValue() : 0L;

                  entities.add(
                      StockMarketLatest.builder()
                          .ticker(ticker)
                          .tradingDate(tradingDate)
                          .market(marketsByTicker.get(ticker))
                          .indexMemberships(indexMembershipService.getIndexMemberships(ticker))
                          .closePrice(
                              data.containsKey("c") ? ((Number) data.get("c")).doubleValue() : 0.0)
                          .totalVolume(
                              data.containsKey("vo") ? ((Number) data.get("vo")).longValue() : 0L)
                          .foreignBuyVol(fBuy)
                          .foreignSellVol(fSell)
                          .foreignNetVol(fBuy - fSell)
                          .rawJsonSnapshot(jsonVal)
                          .build());
                } catch (Exception e) {
                  log.error("[Thread-3] Lỗi xử lý JSON mã: {}", ticker, e);
                }
              }
              if (!entities.isEmpty()) {
                entities.forEach(marketLatestRepository::upsert);
                log.info("[Thread-3] ✓ Đã lưu thành công {} dòng Market Latest.", entities.size());
              }
            },
            etlExecutor);

    CompletableFuture.allOf(tickerTrendTask, foreignTrendTask, marketLatestTask).join();

    // Clear Redis keys used during today's ETL (ticker trends, foreign trends,
    // market latest)
    try {
      log.info("[ETL-Parallel] Clearing Redis keys for date {}", currentDate);
      // Delete per-ticker sorted sets
      for (String ticker : activeTickers) {
        String tKey = "ticker_trend:" + currentDate + ":" + ticker;
        String fKey = "foreign_trend:" + currentDate + ":" + ticker;
        try {
          redisService.delete(tKey);
        } catch (Exception ex) {
          log.warn("[ETL-Parallel] Không thể xóa key {}: {}", tKey, ex.getMessage());
        }
        try {
          redisService.delete(fKey);
        } catch (Exception ex) {
          log.warn("[ETL-Parallel] Không thể xóa key {}: {}", fKey, ex.getMessage());
        }
      }

      // Delete market latest hash for today
      String marketKey = "market:latest:" + currentDate;
      try {
        redisService.delete(marketKey);
      } catch (Exception ex) {
        log.warn("[ETL-Parallel] Không thể xóa key {}: {}", marketKey, ex.getMessage());
      }

      log.info(
          "[ETL-Parallel] Cleared Redis keys for {} tickers and market latest.",
          activeTickers.size());
    } catch (Exception e) {
      log.error("[ETL-Parallel] Lỗi khi xóa cache Redis sau ETL", e);
    }
    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "[🚀 ETL-Parallel] HOÀN TẤT ĐỒNG BỘ ĐA LUỒNG TRONG: {} ms. Toàn bộ dữ liệu an toàn dưới DB!",
        duration);
  }

  /**
   * Hàm phụ trợ: Cắt nhỏ mảng dữ liệu khổng lồ thành từng cụm nhỏ (ví dụ 1000 dòng) để đẩy xuống DB
   */
  private <T> void saveInBatches(
      List<T> entities, java.util.function.Consumer<List<T>> saveAction) {
    for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
      int endIndex = Math.min(i + BATCH_SIZE, entities.size());
      List<T> subList = entities.subList(i, endIndex);
      saveAction.accept(subList);
    }
  }

  private LocalDateTime convertTimestampToDateTime(long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}
