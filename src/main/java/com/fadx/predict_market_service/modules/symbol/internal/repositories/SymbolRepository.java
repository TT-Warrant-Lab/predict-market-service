package com.fadx.predict_market_service.modules.symbol.internal.repositories;

import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, String> {
  List<Symbol> findByBaseSymbol(String baseSymbol);

  List<Symbol> findByType(String type);

  List<Symbol> findByMarketAndType(String market, String type);

  @Transactional
  @Modifying
  @Query(
      "update Symbol s set s.referencePrice = ?1, s.ceilingPrice = ?2, s.floorPrice = ?3 where s.symbol = ?4")
  int updateReferencePriceAndCeilingPriceAndFloorPriceBySymbol(
      BigDecimal referencePrice, BigDecimal ceilingPrice, BigDecimal floorPrice, String symbol);
}
