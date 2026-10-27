package com.fadx.predict_market_service.modules.market.internal;

import static com.fadx.predict_market_service.modules.common.utils.GenericSpecification.build;
import static com.fadx.predict_market_service.modules.common.utils.GenericSpecification.buildSort;
import static com.fadx.predict_market_service.modules.common.utils.GenericSpecification.queryIn;

import com.fadx.predict_market_service.modules.common.entity.CWAnalysis;
import com.fadx.predict_market_service.modules.common.repositories.CWAnalysisRepository;
import com.fadx.predict_market_service.modules.market.api.dtos.AnalysisRequest;
import com.fadx.predict_market_service.modules.market.api.dtos.AnalysisResponse;
import com.fadx.predict_market_service.modules.market.config.WarrantAnalysisConfig;
import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import com.fadx.predict_market_service.modules.symbol.internal.repositories.SymbolRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class WarrantService {

  private final SymbolRepository symbolRepository;
  private final CWAnalysisRepository cwAnalysisRepository;
  private final WarrantAnalysisConfig warrantAnalysisConfig;

  public WarrantService(
      SymbolRepository symbolRepository,
      CWAnalysisRepository cwAnalysisRepository,
      WarrantAnalysisConfig warrantAnalysisConfig) {
    this.symbolRepository = symbolRepository;
    this.cwAnalysisRepository = cwAnalysisRepository;
    this.warrantAnalysisConfig = warrantAnalysisConfig;
  }

  public Map<String, List<Symbol>> getWarrants() {
    return symbolRepository.findByType("CW").stream()
        .collect(Collectors.groupingBy(Symbol::getBaseSymbol));
  }

  public List<Symbol> getWarrantsBySymbol(String baseSymbol) {
    return symbolRepository.findByBaseSymbol(baseSymbol).stream()
        .filter(s -> "CW".equals(s.getType()))
        .sorted(Comparator.comparing(Symbol::getSymbol))
        .collect(Collectors.toList());
  }

  public List<AnalysisResponse> getAnalysis(AnalysisRequest request, int page, int size) {
    // Apply defaults if values are missing
    Specification<@NonNull CWAnalysis> spec = build(request.getFilters());
    if (!CollectionUtils.isEmpty(request.getSymbols())) {
      spec = spec.and(queryIn("baseSymbol", request.getSymbols()));
    }
    Pageable pageable = PageRequest.of(page, size, buildSort(request.getSorts()));
    Page<@NonNull CWAnalysis> analyses = cwAnalysisRepository.findAll(spec, pageable);

    return analyses.stream().map(this::convertToAnalysisResponse).collect(Collectors.toList());
  }

  public WarrantAnalysisConfig getConfig() {
    return warrantAnalysisConfig;
  }

  private AnalysisResponse convertToAnalysisResponse(CWAnalysis analysis) {
    AnalysisResponse response = new AnalysisResponse();
    response.setSymbol(analysis.getSymbol());
    response.setBreakevenIncreasePct(analysis.getBreakevenIncreasePct());
    response.setBreakevenPrice(analysis.getBreakevenPrice());
    response.setReferencePrice(analysis.getReferencePrice());
    response.setSpotPrice(analysis.getSpotPrice());
    if (analysis.getTimeToMaturity() != null) {
      response.setDaysToMaturity(analysis.getTimeToMaturity().multiply(BigDecimal.valueOf(365)));
    }
    response.setWarrantPrice(analysis.getWarrantPrice());
    response.setStatus(analysis.getStatus());
    response.setAnalysisDate(analysis.getAnalysisDate());
    return response;
  }
}
