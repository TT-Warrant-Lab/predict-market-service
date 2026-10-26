package com.fadx.predict_market_service.modules.common.repositories;

import com.fadx.predict_market_service.modules.common.entity.StockForeignTrendRaw;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockForeignTrendRawRepository extends JpaRepository<StockForeignTrendRaw, Long> {
  List<StockForeignTrendRaw> findByTickerAndTradingDateOrderByTradingTimeAsc(
      String ticker, LocalDate date);
}
