package com.fadx.predict_market_service.modules.market.internal;

import com.fadx.predict_market_service.modules.common.repositories.StockMarketLatestRepository;
import com.fadx.predict_market_service.modules.market.api.dtos.MarketStatusResponse;
import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import com.fadx.predict_market_service.modules.symbol.internal.repositories.SymbolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Maintains the top ten gainers and losers for every index declared in indexStockList.json. */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexTopMoversService {

  private static final String INDEX_STOCK_LIST_RESOURCE = "static/indexStockList.json";
  private static final String CACHE_KEY_PREFIX = "market:top-movers:";
  private static final int TOP_LIMIT = 10;
  private static final int CACHE_TTL_SECONDS = 86400;
  private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
  private static final LocalTime OPENING_AUCTION_END = LocalTime.of(9, 15);
  private static final LocalTime LUNCH_BREAK_START = LocalTime.of(11, 30);
  private static final LocalTime LUNCH_BREAK_END = LocalTime.of(13, 0);
  private static final LocalTime CLOSING_AUCTION_START = LocalTime.of(14, 30);
  private static final LocalTime MARKET_CLOSE = LocalTime.of(14, 45);
  private static final DateTimeFormatter DATE_KEY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

  private final RedisService redisService;
  private final ObjectMapper objectMapper;
  private final StockMarketLatestRepository stockMarketLatestRepository;
  private final SymbolRepository symbolRepository;

  private Map<String, List<String>> symbolsByIndex = Map.of();
  private Map<String, Set<String>> indexesBySymbol = Map.of();

  @PostConstruct
  void loadIndexConstituents() {
    try {
      Map<String, List<String>> configuredIndices =
          objectMapper.readValue(
              new ClassPathResource(INDEX_STOCK_LIST_RESOURCE).getInputStream(),
              new TypeReference<LinkedHashMap<String, List<String>>>() {});

      Map<String, List<String>> normalizedSymbolsByIndex = new LinkedHashMap<>();
      Map<String, Set<String>> normalizedIndexesBySymbol = new LinkedHashMap<>();
      configuredIndices.forEach(
          (index, symbols) -> {
            List<String> normalizedSymbols =
                symbols.stream().map(this::normalize).distinct().sorted().toList();
            normalizedSymbolsByIndex.put(normalize(index), normalizedSymbols);
            normalizedSymbols.forEach(
                symbol ->
                    normalizedIndexesBySymbol
                        .computeIfAbsent(symbol, ignored -> new java.util.LinkedHashSet<>())
                        .add(normalize(index)));
          });

      symbolsByIndex = Map.copyOf(normalizedSymbolsByIndex);
      indexesBySymbol = Map.copyOf(normalizedIndexesBySymbol);
      reloadMarketIndexes();
      log.info("Loaded top-mover constituents for indexes={}", symbolsByIndex.keySet());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot load " + INDEX_STOCK_LIST_RESOURCE + " for index top movers", exception);
    }
  }

  public synchronized void reloadMarketIndexes() {
    Map<String, List<String>> refreshedSymbolsByIndex = new LinkedHashMap<>(symbolsByIndex);
    refreshedSymbolsByIndex.remove("HOSE");
    refreshedSymbolsByIndex.remove("HNX");
    refreshedSymbolsByIndex.remove("HNXIndex");

    Map<String, Set<String>> refreshedIndexesBySymbol = new LinkedHashMap<>();
    refreshedSymbolsByIndex.forEach(
        (index, symbols) ->
            symbols.forEach(
                symbol ->
                    refreshedIndexesBySymbol
                        .computeIfAbsent(symbol, ignored -> new java.util.LinkedHashSet<>())
                        .add(index)));
    addMarketIndex("HOSE", refreshedSymbolsByIndex, refreshedIndexesBySymbol);
    addMarketIndex("HNX", refreshedSymbolsByIndex, refreshedIndexesBySymbol);
    addMarketIndex("HNXIndex", refreshedSymbolsByIndex, refreshedIndexesBySymbol);
    symbolsByIndex = Map.copyOf(refreshedSymbolsByIndex);
    indexesBySymbol = Map.copyOf(refreshedIndexesBySymbol);
  }

  private void addMarketIndex(
      String index, Map<String, List<String>> symbols, Map<String, Set<String>> indexes) {
    String market = "HNXIndex".equals(index) ? "HNX" : index;
    List<String> marketSymbols =
        symbolRepository.findByMarketAndType(market, "STOCK").stream()
            .map(Symbol::getSymbol)
            .map(this::normalize)
            .sorted()
            .toList();
    symbols.put(index, marketSymbols);
    marketSymbols.forEach(
        ticker ->
            indexes.computeIfAbsent(ticker, ignored -> new java.util.LinkedHashSet<>()).add(index));
  }

  /**
   * Rebuild only indexes containing {@code ticker}. The source of truth is market:latest:{date},
   * which has already been updated by the Kafka quote pipeline before this method is invoked.
   */
  public void refreshForTicker(String ticker, String currentDate) {
    Set<String> affectedIndexes = indexesBySymbol.get(normalize(ticker));
    if (affectedIndexes == null || affectedIndexes.isEmpty()) {
      return;
    }

    for (String index : affectedIndexes) {
      refreshIndex(index, currentDate);
    }
  }

  public MarketStatusResponse getMarketStatus() {
    MarketSession session = resolveMarketSession();
    return MarketStatusResponse.from(session.marketStatus());
  }

  /** True only while the exchange can produce new market updates. */
  public boolean isLiveMarketSession() {
    return getMarketStatus().marketActive();
  }

  /** True after the closing auction has completed on a trading day. */
  public boolean isAfterMarketClose() {
    return getMarketStatus().marketStatus() == MarketStatusResponse.MarketStatus.AFTER_CLOSE;
  }

  /** Returns only the top-movers list; market state is available from getMarketStatus(). */
  public List<Map<String, Object>> getTopMovers() {
    MarketSession session = resolveMarketSession();
    return switch (session.source()) {
      case NONE -> List.of();
      case CACHE -> getTopMoversFromCache(session.now().toLocalDate().format(DATE_KEY_FORMAT));
      case DATABASE -> getTopMoversFromDatabase(session.now().toLocalDate());
    };
  }

  private MarketSession resolveMarketSession() {
    ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
    DayOfWeek day = now.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      return new MarketSession(MarketStatusResponse.MarketStatus.WEEKEND, DataSource.NONE, now);
    }
    LocalTime time = now.toLocalTime();
    if (time.isBefore(MARKET_OPEN)) {
      return new MarketSession(MarketStatusResponse.MarketStatus.PRE_OPEN, DataSource.NONE, now);
    }
    if (time.isBefore(OPENING_AUCTION_END)) {
      return new MarketSession(
          MarketStatusResponse.MarketStatus.OPENING_AUCTION, DataSource.CACHE, now);
    }
    if (time.isBefore(LUNCH_BREAK_START)) {
      return new MarketSession(MarketStatusResponse.MarketStatus.CONTINUOUS, DataSource.CACHE, now);
    }
    if (time.isBefore(LUNCH_BREAK_END)) {
      return new MarketSession(
          MarketStatusResponse.MarketStatus.LUNCH_BREAK, DataSource.CACHE, now);
    }
    if (time.isBefore(CLOSING_AUCTION_START)) {
      return new MarketSession(MarketStatusResponse.MarketStatus.CONTINUOUS, DataSource.CACHE, now);
    }
    if (time.isBefore(MARKET_CLOSE)) {
      return new MarketSession(
          MarketStatusResponse.MarketStatus.CLOSING_AUCTION, DataSource.CACHE, now);
    }
    return new MarketSession(
        MarketStatusResponse.MarketStatus.AFTER_CLOSE, DataSource.DATABASE, now);
  }

  /** During the live session, API output uses the realtime snapshots maintained by Kafka. */
  private List<Map<String, Object>> getTopMoversFromCache(String currentDate) {
    List<Map<String, Object>> result = new ArrayList<>();
    symbolsByIndex.keySet().stream()
        .sorted()
        .forEach(index -> readCachedSnapshot(currentDate, index, result));
    return result;
  }

  /** After the market closes, API output is rebuilt only from persisted market snapshots. */
  private List<Map<String, Object>> getTopMoversFromDatabase(LocalDate tradingDate) {
    Map<String, String> rawQuotesBySymbol = loadQuotesFromDatabase(tradingDate);
    List<Map<String, Object>> result = new ArrayList<>();

    symbolsByIndex.keySet().stream()
        .sorted()
        .forEach(
            index -> {
              List<String> symbols = symbolsByIndex.get(index);
              result.add(buildResult(index, symbols, rawQuotesBySymbol));
            });
    return result;
  }

  private void readCachedSnapshot(
      String currentDate, String index, List<Map<String, Object>> result) {
    try {
      String cached = redisService.get(CACHE_KEY_PREFIX + currentDate + ":" + index);
      if (cached != null && !cached.isBlank()) {
        result.add(
            objectMapper.readValue(cached, new TypeReference<LinkedHashMap<String, Object>>() {}));
      }
    } catch (Exception exception) {
      log.warn("Unable to read top-movers cache for index={}", index, exception);
    }
  }

  private void refreshIndex(String index, String currentDate) {
    List<String> symbols = symbolsByIndex.get(index);
    if (symbols == null || symbols.isEmpty()) {
      return;
    }

    List<String> rawQuotes =
        redisService.hmget("market:latest:" + currentDate, symbols.toArray(String[]::new));
    Map<String, String> rawQuotesBySymbol = new LinkedHashMap<>();
    for (int i = 0; i < rawQuotes.size(); i++) {
      if (rawQuotes.get(i) != null) {
        rawQuotesBySymbol.put(symbols.get(i), rawQuotes.get(i));
      }
    }
    Map<String, Object> snapshot = buildSnapshot(index, symbols, rawQuotesBySymbol);

    String cacheKey = CACHE_KEY_PREFIX + currentDate + ":" + index;
    Map<String, Object> result = new LinkedHashMap<>();
    result.putAll(snapshot);
    result.put("updatedAt", Instant.now().toEpochMilli());

    try {
      redisService.setWithExpiry(
          cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL_SECONDS);
    } catch (Exception exception) {
      log.error("Unable to cache top movers for index={}", index, exception);
    }
  }

  private Map<String, String> loadQuotesFromDatabase(LocalDate tradingDate) {
    List<String> allSymbols =
        symbolsByIndex.values().stream().flatMap(List::stream).distinct().toList();
    Map<String, String> rawQuotesBySymbol = new LinkedHashMap<>();
    Map<String, com.fadx.predict_market_service.modules.common.entity.StockMarketLatest>
        latestByTicker = new LinkedHashMap<>();
    stockMarketLatestRepository
        .findByTradingDateAndMarketIn(tradingDate, List.of("HOSE", "HNX"))
        .forEach(latest -> latestByTicker.put(normalize(latest.getTicker()), latest));
    // Retain support for rows written before market classification was introduced.
    stockMarketLatestRepository
        .findByTradingDateAndTickerIn(tradingDate, allSymbols)
        .forEach(latest -> latestByTicker.putIfAbsent(normalize(latest.getTicker()), latest));
    latestByTicker
        .values()
        .forEach(
            latest -> {
              if (latest.getRawJsonSnapshot() != null && !latest.getRawJsonSnapshot().isBlank()) {
                rawQuotesBySymbol.put(normalize(latest.getTicker()), latest.getRawJsonSnapshot());
              }
            });
    return rawQuotesBySymbol;
  }

  private Map<String, Object> buildResult(
      String index, List<String> symbols, Map<String, String> rawQuotesBySymbol) {
    Map<String, Object> result =
        new LinkedHashMap<>(buildSnapshot(index, symbols, rawQuotesBySymbol));
    result.put("updatedAt", Instant.now().toEpochMilli());
    return result;
  }

  private Map<String, Object> buildSnapshot(
      String index, List<String> symbols, Map<String, String> rawQuotesBySymbol) {
    List<TopMover> movers =
        symbols.stream()
            .map(symbol -> parseMover(symbol, rawQuotesBySymbol.get(symbol)))
            .filter(java.util.Objects::nonNull)
            .toList();
    List<TopMover> gainers =
        movers.stream()
            .filter(mover -> mover.changePercent() > 0)
            .sorted(
                Comparator.comparingDouble(TopMover::changePercent)
                    .reversed()
                    .thenComparing(TopMover::symbol))
            .limit(TOP_LIMIT)
            .toList();
    List<TopMover> losers =
        movers.stream()
            .filter(mover -> mover.changePercent() < 0)
            .sorted(
                Comparator.comparingDouble(TopMover::changePercent).thenComparing(TopMover::symbol))
            .limit(TOP_LIMIT)
            .toList();
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("index", index);
    snapshot.put("topGainers", gainers);
    snapshot.put("topLosers", losers);
    return snapshot;
  }

  private TopMover parseMover(String expectedSymbol, String rawQuote) {
    if (rawQuote == null || rawQuote.isBlank()) {
      return null;
    }
    try {
      JsonNode quote = objectMapper.readTree(rawQuote).path("p").path(1);
      if (!quote.isObject() || !quote.hasNonNull("r")) {
        return null;
      }
      double changeRate = quote.path("r").asDouble(Double.NaN);
      if (!Double.isFinite(changeRate)) {
        return null;
      }
      String symbol = quote.path("s").asText(expectedSymbol);
      return new TopMover(
          normalize(symbol),
          changeRate * 100D,
          quote.path("ch").asDouble(0D),
          quote.path("c").asDouble(0D));
    } catch (IOException exception) {
      log.warn("Ignoring malformed latest quote for symbol={}", expectedSymbol, exception);
      return null;
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  public record TopMover(String symbol, double changePercent, double change, double price) {}

  private record MarketSession(
      MarketStatusResponse.MarketStatus marketStatus, DataSource source, ZonedDateTime now) {}

  private enum DataSource {
    NONE,
    CACHE,
    DATABASE
  }
}
