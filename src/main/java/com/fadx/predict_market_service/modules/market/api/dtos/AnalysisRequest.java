package com.fadx.predict_market_service.modules.market.api.dtos;

import java.util.List;
import lombok.Data;
import lombok.NonNull;

@Data
public class AnalysisRequest {
  private List<String> symbols;
  @NonNull private List<List<FilterCriteria>> filters;
  private List<SortCriteria> sorts;
}
