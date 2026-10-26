package com.fadx.predict_market_service.modules.common.repositories;

import com.fadx.predict_market_service.modules.common.entity.StockTickerTrendRaw;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTickerTrendRawRepository
    extends JpaRepository<StockTickerTrendRaw, StockTickerTrendRaw.StockTickerId> {
  List<StockTickerTrendRaw> findByTickerAndTradingDateOrderByTradingTimeAsc(
      String ticker, LocalDate date);
}
