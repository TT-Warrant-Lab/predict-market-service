package com.fadx.predict_market_service.modules.common.repositories;

import com.fadx.predict_market_service.modules.common.entity.SymbolMatchHistory;
import com.fadx.predict_market_service.modules.common.entity.SymbolMatchHistoryId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SymbolMatchHistoryRepository
    extends JpaRepository<SymbolMatchHistory, SymbolMatchHistoryId> {

  @Query(
      value =
          "SELECT DISTINCT trading_date FROM raw_symbol_match_history "
              + "WHERE symbol = :symbol ORDER BY trading_date DESC LIMIT 6",
      nativeQuery = true)
  List<LocalDate> findSixLatestTradingDates(@Param("symbol") String symbol);

  @Query(
      "select distinct history.tradingDate from SymbolMatchHistory history "
          + "order by history.tradingDate desc")
  List<LocalDate> findDistinctTradingDatesOrderByTradingDateDesc();

  List<SymbolMatchHistory> findByIdSymbolAndTradingDateOrderByIdTradingTimeAsc(
      String symbol, LocalDate tradingDate);

  /** Idempotent write for Kafka redelivery and concurrent quote workers. */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          insert into raw_symbol_match_history
              (symbol, trading_time, trading_date, open_price, high_price, low_price, close_price, volume, created_at)
          values
              (:symbol, :tradingTime, :tradingDate, :openPrice, :highPrice, :lowPrice, :closePrice, :volume, :createdAt)
          on conflict (symbol, trading_time) do update set
              trading_date = excluded.trading_date,
              open_price = excluded.open_price,
              high_price = excluded.high_price,
              low_price = excluded.low_price,
              close_price = excluded.close_price,
              volume = excluded.volume
          """,
      nativeQuery = true)
  int upsert(
      @Param("symbol") String symbol,
      @Param("tradingTime") LocalDateTime tradingTime,
      @Param("tradingDate") LocalDate tradingDate,
      @Param("openPrice") BigDecimal openPrice,
      @Param("highPrice") BigDecimal highPrice,
      @Param("lowPrice") BigDecimal lowPrice,
      @Param("closePrice") BigDecimal closePrice,
      @Param("volume") BigDecimal volume,
      @Param("createdAt") LocalDateTime createdAt);

  default int upsert(SymbolMatchHistory history) {
    return upsert(
        history.getId().getSymbol(),
        history.getId().getTradingTime(),
        history.getTradingDate(),
        history.getOpenPrice(),
        history.getHighPrice(),
        history.getLowPrice(),
        history.getClosePrice(),
        history.getVolume(),
        LocalDateTime.now());
  }

  @Transactional
  @Modifying
  @Query(
      "delete from SymbolMatchHistory history "
          + "where history.id.symbol = :symbol and history.tradingDate < :retainedFrom")
  int deleteBySymbolAndTradingDateBefore(
      @Param("symbol") String symbol, @Param("retainedFrom") LocalDate retainedFrom);
}
