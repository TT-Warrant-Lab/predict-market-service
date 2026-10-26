package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.modules.market.api.dtos.ForeignChartPointDto;
import com.fadx.predict_market_service.modules.market.api.dtos.ForeignChartResponseDto;
import com.fadx.predict_market_service.modules.common.dto.TickerChartPointDto;
import com.fadx.predict_market_service.modules.common.dto.TickerChartResponseDto;
import com.fadx.predict_market_service.modules.common.entity.StockForeignTrendRaw;
import com.fadx.predict_market_service.modules.common.entity.StockTickerTrendRaw;
import com.fadx.predict_market_service.modules.common.repositories.StockForeignTrendRawRepository;
import com.fadx.predict_market_service.modules.common.repositories.StockTickerTrendRawRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fadx.predict_market_service.modules.market.internal.IndexTopMoversService;
import com.fadx.predict_market_service.modules.market.internal.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.resps.Tuple;

/**
 * Aggregate dữ liệu raw (ghi theo từng tick, giá trị các cột volume là CỘNG DỒN từ đầu phiên) thành
 * chart theo khung 1 phút.
 *
 * <p>QUAN TRỌNG - giả định đang dùng: accumulated_volume, foreign_buy_vol, foreign_sell_vol,
 * foreign_net_vol là giá trị CỘNG DỒN (tăng dần trong ngày), KHÔNG phải snapshot rời rạc từng lần
 * ghi.
 *
 * <p>NGUỒN DỮ LIỆU - chọn theo khung giờ: - Trong giờ giao dịch (09:00-15:00, Thứ 2 - Thứ 6): ưu
 * tiên đọc trực tiếp từ Redis ZSET (ticker_trend:*, foreign_trend:*) vì đây là dữ liệu mới nhất, DB
 * thường chỉ được ETL đổ xuống cuối ngày. Nếu cache rỗng (hiếm, vd vừa mở cửa/redis mới restart)
 * thì fallback đọc DB. - Ngoài giờ giao dịch: ưu tiên đọc DB (đã qua ETL, dữ liệu sạch/đầy đủ hơn).
 * Nếu DB chưa có data cho ngày được hỏi (job ETL chưa chạy / đang chạy dở) thì fallback đọc thẳng
 * từ cache Redis (dữ liệu thô của ngày đó có thể vẫn còn do TTL 24h).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockChartService {

  private static final DateTimeFormatter DATE_KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private final StockTickerTrendRawRepository tickerRepository;
  private final StockForeignTrendRawRepository foreignRepository;
  private final RedisService redisService;
  private final IndexTopMoversService indexTopMoversService;

  public TickerChartResponseDto getIntradayTickerChart(String ticker, LocalDate date) {
    List<StockTickerTrendRaw> raws;

    if (isTradingHoursNow()) {
      raws = fetchTickerFromCache(ticker, date);
      if (raws.isEmpty()) {
        log.debug("[Chart] Cache rỗng cho ticker_trend {} {}, fallback sang DB", ticker, date);
        raws = tickerRepository.findByTickerAndTradingDateOrderByTradingTimeAsc(ticker, date);
      }
    } else {
      raws = tickerRepository.findByTickerAndTradingDateOrderByTradingTimeAsc(ticker, date);
      if (raws.isEmpty()) {
        log.debug(
            "[Chart] DB chưa có data ticker {} {} (có thể chưa ETL), fallback sang cache",
            ticker,
            date);
        raws = fetchTickerFromCache(ticker, date);
      }
    }

    return TickerChartResponseDto.of(ticker, date, buildTickerPoints(raws));
  }

  public ForeignChartResponseDto getIntradayForeignChart(String ticker, LocalDate date) {
    List<StockForeignTrendRaw> raws;

    if (isTradingHoursNow()) {
      raws = fetchForeignFromCache(ticker, date);
      if (raws.isEmpty()) {
        log.debug("[Chart] Cache rỗng cho foreign_trend {} {}, fallback sang DB", ticker, date);
        raws = foreignRepository.findByTickerAndTradingDateOrderByTradingTimeAsc(ticker, date);
      }
    } else {
      raws = foreignRepository.findByTickerAndTradingDateOrderByTradingTimeAsc(ticker, date);
      if (raws.isEmpty()) {
        log.debug(
            "[Chart] DB chưa có data foreign {} {} (có thể chưa ETL), fallback sang cache",
            ticker,
            date);
        raws = fetchForeignFromCache(ticker, date);
      }
    }

    return ForeignChartResponseDto.of(ticker, date, buildForeignPoints(raws));
  }

  // ------------------------------------------------------------------
  // Trading-hours check
  // ------------------------------------------------------------------

  /** Uses the same central market-status rules as the top-movers and status APIs. */
  private boolean isTradingHoursNow() {
    return indexTopMoversService.isLiveMarketSession();
  }

  // ------------------------------------------------------------------
  // Đọc raw data từ Redis cache (ZSET), format giống hệt tầng ghi (producer)
  // "price|volume|matchSide" và "foreignBuy|foreignSell|foreignNet"
  // ------------------------------------------------------------------

  private List<StockTickerTrendRaw> fetchTickerFromCache(String ticker, LocalDate date) {
    String key = "ticker_trend:" + date.format(DATE_KEY_FMT) + ":" + ticker;
    List<Tuple> raw =
        redisService.zrangeByScoreWithScores(
            key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    List<StockTickerTrendRaw> result = new ArrayList<>(raw.size());
    for (Tuple tuple : raw) {
      String[] parts = tuple.getElement().split("\\|");
      if (parts.length < 3) {
        log.warn(
            "[Chart] Bỏ qua entry ticker_trend sai format: key={}, value={}",
            key,
            tuple.getElement());
        continue;
      }
      try {
        result.add(
            StockTickerTrendRaw.builder()
                .ticker(ticker)
                .tradingDate(date)
                .tradingTime(toLocalDateTime((long) tuple.getScore()))
                .price(Double.parseDouble(parts[0]))
                .accumulatedVolume(Long.parseLong(parts[1]))
                .matchSide(parts[2])
                .build());
      } catch (NumberFormatException e) {
        log.warn(
            "[Chart] Lỗi parse entry ticker_trend: key={}, value={}", key, tuple.getElement(), e);
      }
    }
    // ZRANGEBYSCORE trả về tăng dần theo score (timestamp) sẵn -> không cần sort
    // lại.
    return result;
  }

  private List<StockForeignTrendRaw> fetchForeignFromCache(String ticker, LocalDate date) {
    String key = "foreign_trend:" + date.format(DATE_KEY_FMT) + ":" + ticker;
    List<Tuple> raw =
        redisService.zrangeByScoreWithScores(
            key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    List<StockForeignTrendRaw> result = new ArrayList<>(raw.size());
    for (Tuple tuple : raw) {
      String[] parts = tuple.getElement().split("\\|");
      if (parts.length < 3) {
        log.warn(
            "[Chart] Bỏ qua entry foreign_trend sai format: key={}, value={}",
            key,
            tuple.getElement());
        continue;
      }
      try {
        result.add(
            StockForeignTrendRaw.builder()
                .ticker(ticker)
                .tradingDate(date)
                .tradingTime(toLocalDateTime((long) tuple.getScore()))
                .foreignBuyVol(Long.parseLong(parts[0]))
                .foreignSellVol(Long.parseLong(parts[1]))
                .foreignNetVol(Long.parseLong(parts[2]))
                .build());
      } catch (NumberFormatException e) {
        log.warn(
            "[Chart] Lỗi parse entry foreign_trend: key={}, value={}", key, tuple.getElement(), e);
      }
    }
    return result;
  }

  /**
   * Score trong ZSET là timestamp gốc ("ti") của message - giả định đơn vị giây (epoch seconds),
   * khớp với format timestamp dùng ở API historical prices (vd 1776996000). Nếu hệ thống của bạn
   * đang dùng epoch millis cho "ti", đổi Instant.ofEpochSecond thành Instant.ofEpochMilli tại đây.
   */
  private LocalDateTime toLocalDateTime(long epochSeconds) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
  }

  // ------------------------------------------------------------------
  // Aggregation helpers
  // ------------------------------------------------------------------

  private List<TickerChartPointDto> buildTickerPoints(List<StockTickerTrendRaw> raws) {
    if (raws.isEmpty()) {
      return List.of();
    }

    // Giữ lại record cuối cùng của mỗi phút (raws đã sort tăng dần theo tradingTime
    // nên record ghi đè sau cùng trong bucket chính là record mới nhất trong phút
    // đó).
    Map<LocalDateTime, StockTickerTrendRaw> lastRecordPerMinute = new LinkedHashMap<>();
    for (StockTickerTrendRaw r : raws) {
      LocalDateTime bucket = r.getTradingTime().truncatedTo(ChronoUnit.MINUTES);
      lastRecordPerMinute.put(bucket, r);
    }

    List<TickerChartPointDto> points = new ArrayList<>(lastRecordPerMinute.size());
    long prevCumulativeVolume = 0L;

    for (Map.Entry<LocalDateTime, StockTickerTrendRaw> entry : lastRecordPerMinute.entrySet()) {
      StockTickerTrendRaw r = entry.getValue();
      long cumulativeVolume = r.getAccumulatedVolume() == null ? 0L : r.getAccumulatedVolume();
      long volumeInMinute = Math.max(0L, cumulativeVolume - prevCumulativeVolume);

      points.add(
          new TickerChartPointDto(entry.getKey(), r.getPrice(), volumeInMinute, cumulativeVolume));
      prevCumulativeVolume = cumulativeVolume;
    }

    return points;
  }

  private List<ForeignChartPointDto> buildForeignPoints(List<StockForeignTrendRaw> raws) {
    if (raws.isEmpty()) {
      return List.of();
    }

    Map<LocalDateTime, StockForeignTrendRaw> lastRecordPerMinute = new LinkedHashMap<>();
    for (StockForeignTrendRaw r : raws) {
      LocalDateTime bucket = r.getTradingTime().truncatedTo(ChronoUnit.MINUTES);
      lastRecordPerMinute.put(bucket, r);
    }

    List<ForeignChartPointDto> points = new ArrayList<>(lastRecordPerMinute.size());
    long prevBuy = 0L;
    long prevSell = 0L;
    long prevNet = 0L;

    for (Map.Entry<LocalDateTime, StockForeignTrendRaw> entry : lastRecordPerMinute.entrySet()) {
      StockForeignTrendRaw r = entry.getValue();

      long cumulativeBuy = r.getForeignBuyVol() == null ? 0L : r.getForeignBuyVol();
      long cumulativeSell = r.getForeignSellVol() == null ? 0L : r.getForeignSellVol();
      long cumulativeNet = r.getForeignNetVol() == null ? 0L : r.getForeignNetVol();

      long buyInMinute = Math.max(0L, cumulativeBuy - prevBuy);
      long sellInMinute = Math.max(0L, cumulativeSell - prevSell);
      long netInMinute = cumulativeNet - prevNet; // net có thể âm, không clamp về 0

      points.add(
          new ForeignChartPointDto(
              entry.getKey(), buyInMinute, sellInMinute, netInMinute, cumulativeNet));

      prevBuy = cumulativeBuy;
      prevSell = cumulativeSell;
      prevNet = cumulativeNet;
    }

    return points;
  }
}
