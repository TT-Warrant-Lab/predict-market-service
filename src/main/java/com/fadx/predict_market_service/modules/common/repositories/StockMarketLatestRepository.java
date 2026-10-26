package com.fadx.predict_market_service.modules.common.repositories;

import com.fadx.predict_market_service.modules.common.entity.StockMarketLatest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StockMarketLatestRepository extends JpaRepository<StockMarketLatest, Long> {
  Optional<StockMarketLatest> findByTickerAndTradingDate(String ticker, LocalDate tradingDate);

  List<StockMarketLatest> findByTradingDateAndTickerIn(
      LocalDate tradingDate, Collection<String> tickers);

  List<StockMarketLatest> findByTradingDateAndMarketIn(
      LocalDate tradingDate, Collection<String> markets);

  /** Atomically creates or refreshes a daily market snapshot. */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          insert into t_stock_market_latest
              (ticker, trading_date, market, index_memberships, close_price, total_volume,
               foreign_buy_vol, foreign_sell_vol, foreign_net_vol, raw_json_snapshot, created_at)
          values
              (:ticker, :tradingDate, :market, :indexMemberships, :closePrice, :totalVolume,
               :foreignBuyVol, :foreignSellVol, :foreignNetVol, :rawJsonSnapshot, :createdAt)
          on conflict (ticker, trading_date) do update set
              market = excluded.market,
              index_memberships = excluded.index_memberships,
              close_price = excluded.close_price,
              total_volume = excluded.total_volume,
              foreign_buy_vol = excluded.foreign_buy_vol,
              foreign_sell_vol = excluded.foreign_sell_vol,
              foreign_net_vol = excluded.foreign_net_vol,
              raw_json_snapshot = excluded.raw_json_snapshot
          """,
      nativeQuery = true)
  int upsert(
      @Param("ticker") String ticker,
      @Param("tradingDate") LocalDate tradingDate,
      @Param("market") String market,
      @Param("indexMemberships") String indexMemberships,
      @Param("closePrice") Double closePrice,
      @Param("totalVolume") Long totalVolume,
      @Param("foreignBuyVol") Long foreignBuyVol,
      @Param("foreignSellVol") Long foreignSellVol,
      @Param("foreignNetVol") Long foreignNetVol,
      @Param("rawJsonSnapshot") String rawJsonSnapshot,
      @Param("createdAt") LocalDateTime createdAt);

  @Transactional
  default int upsert(StockMarketLatest latest) {
    return upsert(
        latest.getTicker(),
        latest.getTradingDate(),
        latest.getMarket(),
        latest.getIndexMemberships(),
        latest.getClosePrice(),
        latest.getTotalVolume(),
        latest.getForeignBuyVol(),
        latest.getForeignSellVol(),
        latest.getForeignNetVol(),
        latest.getRawJsonSnapshot(),
        LocalDateTime.now());
  }
}
