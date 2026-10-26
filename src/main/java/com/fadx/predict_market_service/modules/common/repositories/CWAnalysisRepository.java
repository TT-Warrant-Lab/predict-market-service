package com.fadx.predict_market_service.modules.common.repositories;

import com.fadx.predict_market_service.modules.common.entity.CWAnalysis;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CWAnalysisRepository
    extends JpaRepository<CWAnalysis, String>, JpaSpecificationExecutor<CWAnalysis> {

  @Query(
      "SELECT c FROM CWAnalysis c WHERE "
          + "(:symbols IS NULL OR c.baseSymbol IN :symbols) AND "
          + "(:minTimeToMaturity IS NULL OR c.timeToMaturity > :minTimeToMaturity) AND "
          + "(:maxBreakevenIncreasePct IS NULL OR c.breakevenIncreasePct < :maxBreakevenIncreasePct)")
  List<CWAnalysis> findByCriteria(
      @Param("symbols") List<String> symbols,
      @Param("minTimeToMaturity") BigDecimal minTimeToMaturity,
      @Param("maxBreakevenIncreasePct") BigDecimal maxBreakevenIncreasePct);

  List<CWAnalysis> findByBaseSymbol(String baseSymbol);

  @Transactional
  void deleteByAnalysisDateBefore(LocalDate date);
}
