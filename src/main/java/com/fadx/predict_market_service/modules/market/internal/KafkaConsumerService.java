package com.fadx.predict_market_service.modules.market.internal;

import com.fadx.predict_market_service.core.api.RealtimePublisher;
import com.fadx.predict_market_service.modules.common.dto.TickerChartPointDto;
import com.fadx.predict_market_service.modules.common.entity.StockMarketLatest;
import com.fadx.predict_market_service.modules.common.entity.SymbolMatchHistory;
import com.fadx.predict_market_service.modules.common.entity.SymbolMatchHistoryId;
import com.fadx.predict_market_service.modules.common.repositories.StockMarketLatestRepository;
import com.fadx.predict_market_service.modules.common.repositories.SymbolMatchHistoryRepository;
import com.fadx.predict_market_service.modules.common.repositories.SymbolPriceHisRepository;
import com.fadx.predict_market_service.modules.common.services.IndexMembershipService;
import com.fadx.predict_market_service.modules.market.api.dtos.ForeignChartPointDto;
import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import com.fadx.predict_market_service.modules.symbol.internal.repositories.SymbolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

  private static final int QUOTE_WORKER_COUNT = 5;
  private static final int QUOTE_QUEUE_CAPACITY = 1000;

  private static final String KAFKA_TREND_TOPIC = "market.trend";
  private static final String STRATEGY_SIGNALS_TOPIC = "signals";
  private static final String USER_NOTIFICATION_TOPIC = "user.notification";
  private static final String TREND_TYPE_TICKER = "ticker";
  private static final String TREND_TYPE_FOREIGN = "foreign";
  private static final Map<String, Double> INDEX_REF_CAP_MAP =
      Map.of(
          "VN-INDEX",
          4.7123e12, // Sàn HOSE (~4.71 triệu
          // tỷ VND)
          "VN30",
          3.2000e12, // Rổ VN30
          "HNXIndex",
          0.3200e12, // Sàn HNX
          "HNX30",
          0.2100e12 // Rổ HNX30
          );

  // Danh sách cấu hình các Chỉ số (Indices)
  private static final Map<String, String> INDICES_MAP =
      Map.of(
          "VN-INDEX",
          "VN-INDEX",
          "VN30",
          "VN30",
          "VN100",
          "VN100",
          "HNXIndex",
          "HNXIndex",
          "HNX30",
          "HNX30",
          "HNXUpcomIndex",
          "HNXUpcomIndex");

  private final ObjectMapper objectMapper;
  private final RedisService redisService;
  private final IndexTopMoversService indexTopMoversService;
  private final IndexMembershipService indexMembershipService;
  private final WebsocketBroadcastService broadcastService;
  private final RealtimePublisher realtimePublisher;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final StockMarketLatestRepository stockMarketLatestRepository;
  private final SymbolMatchHistoryRepository symbolMatchHistoryRepository;
  private final SymbolRepository symbolRepository;
  private final SymbolPriceHisRepository symbolPriceHisRepository;
  private final Map<String, TickerVolumeState> tickerVolumeState = new ConcurrentHashMap<>();
  private final Map<String, ForeignVolumeState> foreignVolumeState = new ConcurrentHashMap<>();
  private final Map<String, LocalDate> matchHistoryRetentionDateByTicker =
      new ConcurrentHashMap<>();
  private final ThreadPoolExecutor quoteWorkers =
      new ThreadPoolExecutor(
          QUOTE_WORKER_COUNT,
          QUOTE_WORKER_COUNT,
          0L,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(QUOTE_QUEUE_CAPACITY),
          runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("quote-worker-" + QUOTE_THREAD_NUMBER.incrementAndGet());
            thread.setDaemon(false);
            return thread;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private static final AtomicInteger QUOTE_THREAD_NUMBER = new AtomicInteger();

  /**
   * Receives strategy signals emitted by strategy-engine.
   *
   * <p>The signal contract has not been finalized in this repository, so the message is parsed as a
   * JSON tree and kept intact. This prevents predict-service from silently dropping fields that
   * strategy-engine may add before a shared, versioned contract is introduced.
   */
  @KafkaListener(topics = STRATEGY_SIGNALS_TOPIC, groupId = "prediction-group")
  public void listenStrategySignals(ConsumerRecord<String, String> record) {
    String rawPayload = record.value();
    if (rawPayload == null || rawPayload.isBlank()) {
      log.warn(
          "[kafka] Ignoring empty strategy signal topic={} partition={} offset={}",
          record.topic(),
          record.partition(),
          record.offset());
      return;
    }

    try {
      processStrategySignal(objectMapper.readTree(rawPayload));
    } catch (Exception exception) {
      log.error(
          "[kafka] Failed to parse strategy signal topic={} partition={} offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          exception);
    }
  }

  private void processStrategySignal(JsonNode signalPayload) {
    /*
     * TODO: Complete the notification workflow before enabling delivery:
     *
     * 1. Define a shared, versioned signal contract with strategy-engine. At minimum it should
     *    contain eventId, occurredAt, strategyId, strategyVersion, symbol, side, price and the
     *    strategy owner identifier. Reject unsupported versions and malformed required fields.
     *
     * 2. Make eventId idempotent. Kafka delivery is at-least-once, so add a unique constraint or
     *    inbox table keyed by eventId. A retried record must not create or publish a duplicate
     *    notification.
     *
     * 3. Resolve the authoritative strategy owner from predict-service's database by strategyId.
     *    Do not trust the email/user value supplied by strategy-engine as the only authorization
     *    source. If the event also contains an owner, compare it with the database owner and reject
     *    mismatches as stale or forged events.
     *
     * 4. Check the owner's subscription at processing time, not at strategy creation/evaluation
     *    time. The lookup must verify that the plan is PREMIUM and currently active (start/end,
     *    cancellation and suspension state). A cache may be used, but it needs a short TTL plus
     *    invalidation on upgrade, downgrade, cancellation and expiry so revoked access is not
     *    served from stale cache.
     *
     * 5. Decide and persist the non-premium policy explicitly: either drop with an audit reason or
     *    store a suppressed notification. Never publish user.notification for an ineligible owner.
     *
     * 6. For an eligible owner, sanitize the client-facing payload and persist the notification
     *    with eventId, owner/user id, type, title/body or structured payload, read status and
     *    timestamps. Do not expose internal rule traces, credentials or provider payloads.
     *
     * 7. Persist the notification and an outbox event in one database transaction. A separate
     *    outbox publisher should call publishUserNotification(...) only after commit. Publishing
     *    directly before/inside a DB transaction can lose a notification or send one that later
     *    rolls back.
     *
     * 8. Use the normalized owner email (or preferably an immutable user id once the gateway
     *    contract supports it) as the Kafka record key. This preserves per-user ordering and lets
     *    ws-gateway route the event only to authenticated sessions belonging to that user.
     *
     * 9. Add retry/DLT handling and metrics for malformed, duplicate, owner-mismatch, non-premium,
     *    persisted, published and failed events. Do not log the full payload if it can contain
     *    personal or sensitive strategy data.
     *
     * Intended final call after the checks and successful persistence/outbox hand-off:
     * publishUserNotification(ownerEmail, clientPayload);
     */
    log.debug("[kafka] Strategy signal received; notification workflow is not enabled yet");
  }

  /**
   * Publishes a notification for ws-gateway.
   *
   * <p>Kafka key is the normalized user email so all notifications for one user remain ordered. The
   * JSON value contract is: {@code {"user":"a@x.com","payload":{...}}}.
   */
  public void publishUserNotification(String user, JsonNode payload) {
    if (user == null || user.isBlank()) {
      throw new IllegalArgumentException("Notification user must not be blank");
    }
    if (payload == null || payload.isNull()) {
      throw new IllegalArgumentException("Notification payload must not be null");
    }

    String normalizedUser = user.trim().toLowerCase(Locale.ROOT);
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("user", normalizedUser);
    message.put("payload", payload);
    kafkaTemplate.send(USER_NOTIFICATION_TOPIC, normalizedUser, message);
  }

  @KafkaListener(topics = "analysis.warrant", groupId = "prediction-group")
  public void listenAnalysisWarrant(ConsumerRecord<String, String> record) {
    String rawPayload = record.value() != null ? record.value() : "";
    broadcastService.onWarrantAnalysis(rawPayload);
  }

  @KafkaListener(topicPattern = "market\\.bidoffer(\\..+)?", groupId = "prediction-group")
  public void listenBidoffer(ConsumerRecord<String, String> record) {
    String rawPayload = record.value() != null ? record.value() : "";
    broadcastService.onMarketUpdate(rawPayload);
  }

  @KafkaListener(topicPattern = "market\\.quote(\\..+)?", groupId = "prediction-group")
  public void listenQuote(ConsumerRecord<String, String> record) {
    String rawPayload = record.value() != null ? record.value() : "";

    if (rawPayload.isEmpty()) {
      return;
    }

    try {
      quoteWorkers.execute(
          () ->
              processQuoteInWorker(
                  rawPayload,
                  record.topic(),
                  record.partition(),
                  record.offset(),
                  record.key(),
                  record.headers()));
    } catch (RejectedExecutionException e) {
      log.error(
          "[kafka] Quote worker queue is full; message rejected topic={} partition={} offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          e);
    }
  }

  private void processQuoteInWorker(
      String rawPayload, String topic, int partition, long offset, String key, Headers headers) {
    try {
      broadcastService.onMarketUpdate(rawPayload);
      processAndStoreToRedis(rawPayload);
    } catch (Exception e) {
      log.error(
          "[kafka] Quote worker failed topic={} partition={} offset={}: {}",
          topic,
          partition,
          offset,
          e.getMessage(),
          e);
    }
  }

  @PreDestroy
  public void shutdownQuoteWorkers() {
    quoteWorkers.shutdown();
    try {
      if (!quoteWorkers.awaitTermination(10, TimeUnit.SECONDS)) {
        quoteWorkers.shutdownNow();
      }
    } catch (InterruptedException e) {
      quoteWorkers.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private String formatHeaders(Headers headers) {
    if (headers == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    headers.forEach(
        h -> {
          if (!sb.isEmpty()) {
            sb.append(",");
          }
          sb.append(h.key()).append(":").append(h.value() == null ? "" : new String(h.value()));
        });
    return sb.toString();
  }

  /** Bóc tách JSON quote, phân loại Index/Stock để tính Divisor/Impact và đẩy Redis & Websocket */
  private void processAndStoreToRedis(String jsonPayload) {
    try {
      Map<String, Object> root =
          objectMapper.readValue(jsonPayload, new TypeReference<Map<String, Object>>() {});

      if (!root.containsKey("p")) return;
      List<Object> pList = (List<Object>) root.get("p");

      if (pList != null && pList.size() >= 2 && pList.get(1) instanceof Map) {
        Map<String, Object> data = (Map<String, Object>) pList.get(1);

        // Yêu cầu tối thiểu: có mã 's' và giá 'c'
        if (data.containsKey("s") && data.containsKey("c")) {

          String ticker = (String) data.get("s");
          double currentPrice = ((Number) data.get("c")).doubleValue();
          double priceChange =
              data.containsKey("ch") ? ((Number) data.get("ch")).doubleValue() : 0.0;

          persistRealtimeMarketData(ticker, data, jsonPayload);

          // =====================================================================
          // NHÁNH 1: TÍNH DIVISOR NẾU LÀ INDEX
          // NHÁNH 2: TÍNH IMPACT NẾU LÀ CỔ PHIẾU THƯỜNG
          // =====================================================================
          // if (INDICES_MAP.containsKey(ticker)) {
          // String indexId = INDICES_MAP.get(ticker);
          // processIndexDivisor(indexId, currentPrice, priceChange);
          // } else {
          // processStockImpact(ticker, priceChange);
          // }

          // Nếu quote không chứa 'vo' (volume) thì không cần lưu lịch sử trend bên dưới
          if (!data.containsKey("vo")) return;

          long timestamp =
              data.containsKey("ti")
                  ? ((Number) data.get("ti")).longValue()
                  : System.currentTimeMillis();
          persistMatchHistory(ticker, data, timestamp);

          // 1. Dữ liệu khối ngoại
          long foreignBuy =
              data.containsKey("frBvo") ? ((Number) data.get("frBvo")).longValue() : 0L;
          long foreignSell =
              data.containsKey("frSvo") ? ((Number) data.get("frSvo")).longValue() : 0L;
          long foreignNet = foreignBuy - foreignSell;

          // 1b. Giá open/high/low (fallback về currentPrice nếu payload không có) + build
          // OHLC json
          double openPrice =
              data.containsKey("o") ? ((Number) data.get("o")).doubleValue() : currentPrice;
          double highPrice =
              data.containsKey("h") ? ((Number) data.get("h")).doubleValue() : currentPrice;
          double lowPrice =
              data.containsKey("l") ? ((Number) data.get("l")).doubleValue() : currentPrice;
          String ohlcJson;
          try {
            Map<String, Object> ohlcMap = new LinkedHashMap<>();
            ohlcMap.put("o", openPrice);
            ohlcMap.put("h", highPrice);
            ohlcMap.put("l", lowPrice);
            ohlcMap.put("c", currentPrice);
            ohlcJson = objectMapper.writeValueAsString(ohlcMap);
          } catch (Exception e) {
            ohlcJson = "{}";
          }

          // 2. Dữ liệu tổng thị trường
          long totalVolume = ((Number) data.get("vo")).longValue();
          String matchSide = data.getOrDefault("mb", "NONE").toString();

          String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

          LocalDateTime minuteBucket =
              LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                  .truncatedTo(ChronoUnit.MINUTES);

          // =====================================================================
          // 1. foreign_trend (ZSET)
          // =====================================================================
          String foreignTrendKey = "foreign_trend:" + currentDate + ":" + ticker;
          // format: foreignBuy|foreignSell|foreignNet|currentPrice|ohlcJson
          String foreignValueStr =
              String.format(
                  Locale.US,
                  "%d|%d|%d|%.2f|%s",
                  foreignBuy,
                  foreignSell,
                  foreignNet,
                  currentPrice,
                  ohlcJson);

          redisService.zadd(foreignTrendKey, timestamp, foreignValueStr);
          redisService.expireIfAbsent(foreignTrendKey, 86400);

          try {
            long[] foreignDelta =
                computeForeignDelta(ticker, currentDate, foreignBuy, foreignSell, foreignNet);
            ForeignChartPointDto foreignPoint =
                new ForeignChartPointDto(
                    minuteBucket, foreignDelta[0], foreignDelta[1], foreignDelta[2], foreignNet);
            realtimePublisher.publish(ticker, foreignPoint, TREND_TYPE_FOREIGN + "." + ticker);
          } catch (Exception e) {
            log.error("[Kafka-Trend] Gửi foreign trend thất bại cho ticker={}", ticker, e);
          }

          // =====================================================================
          // 2. market:latest (HSET)
          // =====================================================================
          String hashKey = "market:latest:" + currentDate;

          redisService.hset(hashKey, ticker, jsonPayload);
          redisService.expireIfAbsent(hashKey, 86400);
          indexTopMoversService.refreshForTicker(ticker, currentDate);

          // =====================================================================
          // 3. ticker_trend (ZSET)
          // =====================================================================
          String tickerTrendKey = "ticker_trend:" + currentDate + ":" + ticker;

          // Tính khối lượng khớp tại thời điểm này (match_volume) TRƯỚC khi format chuỗi
          // lưu Redis
          long tickerVolumeDelta = computeTickerDelta(ticker, currentDate, totalVolume);

          // format: price|accumulatedVolume|matchSide|matchVolume
          String tickerValueStr =
              String.format(
                  Locale.US,
                  "%.1f|%d|%s|%d",
                  currentPrice,
                  totalVolume,
                  matchSide,
                  tickerVolumeDelta);

          redisService.zadd(tickerTrendKey, timestamp, tickerValueStr);
          redisService.expireIfAbsent(tickerTrendKey, 86400);

          try {
            TickerChartPointDto tickerPoint =
                new TickerChartPointDto(minuteBucket, currentPrice, tickerVolumeDelta, totalVolume);
            realtimePublisher.publish(
                ticker, tickerPoint, KAFKA_TREND_TOPIC + "." + TREND_TYPE_TICKER);
          } catch (Exception e) {
            log.error("[Kafka-Trend] Gửi ticker trend thất bại cho ticker={}", ticker, e);
          }
        }
      }
    } catch (Exception e) {
      log.error("[Redis-Pipeline] Failed to process quote message: {}", e.getMessage(), e);
    }
  }

  private void persistRealtimeMarketData(String ticker, Map<String, Object> data, String rawJson) {
    try {
      LocalDate tradingDate = LocalDate.now();
      double close = ((Number) data.getOrDefault("c", 0)).doubleValue();
      long volume = ((Number) data.getOrDefault("vo", 0)).longValue();
      long foreignBuy = ((Number) data.getOrDefault("frBvo", 0)).longValue();
      long foreignSell = ((Number) data.getOrDefault("frSvo", 0)).longValue();

      StockMarketLatest latest =
          StockMarketLatest.builder().ticker(ticker).tradingDate(tradingDate).build();
      latest.setClosePrice(close);
      latest.setMarket(symbolRepository.findById(ticker).map(Symbol::getMarket).orElse(null));
      latest.setIndexMemberships(indexMembershipService.getIndexMemberships(ticker));
      latest.setTotalVolume(volume);
      latest.setForeignBuyVol(foreignBuy);
      latest.setForeignSellVol(foreignSell);
      latest.setForeignNetVol(foreignBuy - foreignSell);
      latest.setRawJsonSnapshot(rawJson);
      stockMarketLatestRepository.upsert(latest);

      if (indexTopMoversService.isAfterMarketClose()) {
        symbolPriceHisRepository.upsert(
            ticker,
            tradingDate,
            decimal(data.get("o"), close),
            decimal(data.get("h"), close),
            decimal(data.get("l"), close),
            java.math.BigDecimal.valueOf(close),
            volume,
            decimal(data.get("re"), close));
      }
    } catch (Exception exception) {
      log.error("Failed to persist realtime market data for ticker={}", ticker, exception);
    }
  }

  private void persistMatchHistory(String ticker, Map<String, Object> data, long timestamp) {
    try {
      double close = ((Number) data.getOrDefault("c", 0D)).doubleValue();
      LocalDateTime tradingTime =
          LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("Asia/Ho_Chi_Minh"));
      SymbolMatchHistoryId id = new SymbolMatchHistoryId();
      id.setSymbol(ticker);
      id.setTradingTime(tradingTime);

      SymbolMatchHistory history = new SymbolMatchHistory();
      history.setId(id);
      history.setTradingDate(tradingTime.toLocalDate());
      history.setOpenPrice(decimal(data.get("o"), close));
      history.setHighPrice(decimal(data.get("h"), close));
      history.setLowPrice(decimal(data.get("l"), close));
      history.setClosePrice(java.math.BigDecimal.valueOf(close));
      history.setVolume(decimal(data.get("vo"), 0D));
      symbolMatchHistoryRepository.upsert(history);

      LocalDate tradingDate = tradingTime.toLocalDate();
      LocalDate previousRetentionDate =
          matchHistoryRetentionDateByTicker.putIfAbsent(ticker, tradingDate);
      if (previousRetentionDate == null || !previousRetentionDate.equals(tradingDate)) {
        matchHistoryRetentionDateByTicker.put(ticker, tradingDate);
        retainFiveMatchHistoryTradingDays(ticker);
      }
    } catch (Exception exception) {
      log.error("Failed to persist match history for ticker={}", ticker, exception);
    }
  }

  private void retainFiveMatchHistoryTradingDays(String ticker) {
    List<LocalDate> retainedDays = symbolMatchHistoryRepository.findSixLatestTradingDates(ticker);
    if (retainedDays.size() == 6) {
      symbolMatchHistoryRepository.deleteBySymbolAndTradingDateBefore(
          ticker, retainedDays.get(retainedDays.size() - 1));
    }
  }

  private java.math.BigDecimal decimal(Object value, double fallback) {
    return java.math.BigDecimal.valueOf(value instanceof Number n ? n.doubleValue() : fallback);
  }

  /** Tính toán Divisor riêng biệt cho TỪNG Chỉ số từ c, ch của chính chỉ số đó */
  private void processIndexDivisor(String indexId, double currentPrice, double priceChange) {
    try {
      double indexRefPrice = currentPrice - priceChange; // RefPrice = c - ch
      if (indexRefPrice <= 0) return;

      // Lấy đúng RefCap của CHÍNH CHỈ SỐ ĐÓ
      Double totalRefCap = INDEX_REF_CAP_MAP.get(indexId);
      if (totalRefCap == null || totalRefCap <= 0) return;

      // Divisor của chỉ số đó = TotalRefCap / RefPrice
      double divisor = totalRefCap / indexRefPrice;

      String divisorKey = "divisor." + indexId;

      // Cache Divisor riêng cho từng index: divisor.VN-INDEX, divisor.VN30,
      // divisor.HNXIndex...
      redisService.set(divisorKey, String.valueOf(divisor));
      log.debug("[Divisor-Engine] Updated divisor.{} = {}", indexId, divisor);
    } catch (Exception e) {
      log.error(
          "[Divisor-Engine] Failed to calculate divisor for indexId={}: {}",
          indexId,
          e.getMessage());
    }
  }

  /** Tính toán Impact cho Cổ phiếu, lưu cache index-impact.{symbol} và bắn WebSocket */
  private void processStockImpact(String symbol, double priceChange) {
    try {
      // 1. Lấy KLCPNY của cổ phiếu
      long shares = getStockListedShares(symbol);
      if (shares <= 0) return;

      // 2. Tính Delta Cap = ch * shares
      double deltaCap = priceChange * shares;

      // 3. Lấy Divisor của VN-INDEX (đã được tính và lưu bởi processIndexDivisor)
      String targetIndex = "VN-INDEX";
      String divisorStr = redisService.get("divisor." + targetIndex);

      if (divisorStr != null && !divisorStr.trim().isEmpty()) {
        double divisor = Double.parseDouble(divisorStr);

        if (divisor > 0) {
          // 4. Impact = DeltaCap / Divisor
          double impact = deltaCap / divisor;

          // 5. Lưu Cache Redis với key: index-impact.{symbol}
          String impactKey = "index-impact." + symbol;
          redisService.set(impactKey, String.valueOf(impact));

          // 6. Bắn tin WebSocket với topic: index-impact.{symbol}
          Map<String, Object> impactPayload =
              Map.of(
                  "symbol",
                  symbol,
                  "impact",
                  impact,
                  "deltaCap",
                  deltaCap,
                  "timestamp",
                  System.currentTimeMillis());

          realtimePublisher.publish(symbol, impactPayload, "index-impact." + symbol);
        }
      }
    } catch (Exception e) {
      log.error(
          "[Impact-Engine] Failed to calculate impact for symbol={}: {}", symbol, e.getMessage());
    }
  }

  /** Helper: Lấy số lượng cổ phiếu niêm yết (KLCPNY) từ Redis Hash: market:stock_static */
  private long getStockListedShares(String symbol) {
    List<String> values = redisService.hmget("market:stock_static", symbol);
    if (values != null && !values.isEmpty() && values.get(0) != null) {
      return Long.parseLong(values.get(0));
    }
    return 0L;
  }

  private long computeTickerDelta(String ticker, String currentDate, long cumulativeVolume) {
    TickerVolumeState state =
        tickerVolumeState.computeIfAbsent(ticker, k -> new TickerVolumeState());
    synchronized (state) {
      if (!currentDate.equals(state.date)) {
        state.date = currentDate;
        state.lastCumulativeVolume = 0L;
      }
      long delta = Math.max(0L, cumulativeVolume - state.lastCumulativeVolume);
      state.lastCumulativeVolume = cumulativeVolume;
      return delta;
    }
  }

  private long[] computeForeignDelta(
      String ticker,
      String currentDate,
      long cumulativeBuy,
      long cumulativeSell,
      long cumulativeNet) {
    ForeignVolumeState state =
        foreignVolumeState.computeIfAbsent(ticker, k -> new ForeignVolumeState());
    synchronized (state) {
      if (!currentDate.equals(state.date)) {
        state.date = currentDate;
        state.lastBuy = 0L;
        state.lastSell = 0L;
        state.lastNet = 0L;
      }
      long buyDelta = Math.max(0L, cumulativeBuy - state.lastBuy);
      long sellDelta = Math.max(0L, cumulativeSell - state.lastSell);
      long netDelta = cumulativeNet - state.lastNet;

      state.lastBuy = cumulativeBuy;
      state.lastSell = cumulativeSell;
      state.lastNet = cumulativeNet;

      return new long[] {buyDelta, sellDelta, netDelta};
    }
  }

  private static class TickerVolumeState {
    String date;
    long lastCumulativeVolume;
  }

  private static class ForeignVolumeState {
    String date;
    long lastBuy;
    long lastSell;
    long lastNet;
  }
}
