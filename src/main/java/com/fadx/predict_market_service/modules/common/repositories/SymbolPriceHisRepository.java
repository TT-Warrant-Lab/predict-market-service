package com.fadx.predict_market_service.modules.common.repositories;

import com.fadx.predict_market_service.modules.common.dto.OhlcvProjection;
import com.fadx.predict_market_service.modules.common.entity.SymbolPriceHis;
import com.fadx.predict_market_service.modules.common.entity.SymbolPriceHisId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SymbolPriceHisRepository extends JpaRepository<SymbolPriceHis, SymbolPriceHisId> {

  /** Atomically creates or refreshes the end-of-day OHLCV snapshot for a symbol. */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          insert into raw_symbol_price_his
              (symbol, trading_date, open_price, high_price, low_price, close_price, volume,
               reference_price)
          values
              (:symbol, :tradingDate, :openPrice, :highPrice, :lowPrice, :closePrice, :volume,
               :referencePrice)
          on conflict (symbol, trading_date) do update set
              open_price = excluded.open_price,
              high_price = excluded.high_price,
              low_price = excluded.low_price,
              close_price = excluded.close_price,
              volume = excluded.volume,
              reference_price = excluded.reference_price
          """,
      nativeQuery = true)
  int upsert(
      @Param("symbol") String symbol,
      @Param("tradingDate") LocalDate tradingDate,
      @Param("openPrice") java.math.BigDecimal openPrice,
      @Param("highPrice") java.math.BigDecimal highPrice,
      @Param("lowPrice") java.math.BigDecimal lowPrice,
      @Param("closePrice") java.math.BigDecimal closePrice,
      @Param("volume") Long volume,
      @Param("referencePrice") java.math.BigDecimal referencePrice);

  List<SymbolPriceHis> findTop10ByIdSymbolOrderByIdTradingDateDesc(String baseSymbol);

  Optional<SymbolPriceHis> findTopByIdSymbolOrderByIdTradingDateDesc(String symbol);

  // Thêm phương thức này
  @Query(
      value =
          "SELECT * FROM ("
              + "    SELECT sph.*, ROW_NUMBER() OVER(PARTITION BY symbol ORDER BY trading_date DESC) as rn "
              + "    FROM raw_symbol_price_his sph "
              + "    WHERE sph.symbol IN (:symbols)"
              + ") tmp WHERE rn = 1",
      nativeQuery = true)
  List<SymbolPriceHis> findLatestPricesForSymbols(@Param("symbols") List<String> symbols);

  @Query(
      value =
          """
                SELECT *
                FROM raw_symbol_price_his
                WHERE symbol = :symbol
                ORDER BY trading_date DESC
                LIMIT 1
            """,
      nativeQuery = true)
  SymbolPriceHis findOlderPricesForSymbol(@Param("symbol") String symbol);

  @Query(
      value =
          """
            SELECT
                trading_date        AS tradingDate,
                open_price          AS openPrice,
                high_price          AS highPrice,
                low_price           AS lowPrice,
                close_price         AS closePrice,
                volume              AS volume,
                reference_price     AS referencePrice
            FROM raw_symbol_price_his
            WHERE symbol = :symbol
                AND trading_date >= :fromDate
                AND trading_date < :toDate
            ORDER BY trading_date DESC
            """,
      nativeQuery = true)
  List<OhlcvProjection> findOhlcvBySymbolAndDateRange(
      @Param("symbol") String symbol,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDatePlusOneDay);

  @Query(
      value =
          """
            SELECT
                array_agg(EXTRACT(EPOCH FROM trading_date)::bigint ORDER BY trading_date) AS t,
                array_agg(open_price ORDER BY trading_date) AS o,
                array_agg(high_price ORDER BY trading_date) AS h,
                array_agg(low_price ORDER BY trading_date) AS l,
                array_agg(close_price ORDER BY trading_date) AS c,
                array_agg(volume ORDER BY trading_date) AS v
            FROM raw_symbol_price_his
            WHERE symbol = :symbol
              AND trading_date >= :fromDate
              AND trading_date < :toDate
            """,
      nativeQuery = true)
  Map<String, Object> findOhlcvColumnar(
      @Param("symbol") String symbol,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDatePlusOneDay);
}
