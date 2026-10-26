package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.modules.market.api.dtos.AnalysisRequest;
import com.fadx.predict_market_service.modules.market.api.dtos.AnalysisResponse;
import com.fadx.predict_market_service.modules.market.internal.WarrantService;
import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warrants")
@Tag(name = "Anonymous | Warrants", description = "API for warrants")
public class WarrantController {

  private final WarrantService warrantService;

  public WarrantController(WarrantService warrantService) {
    this.warrantService = warrantService;
  }

  @GetMapping
  @Operation(summary = "Get Warrants", description = "Retrieve a map of warrants")
  public Map<String, List<Symbol>> getWarrants() {
    return warrantService.getWarrants();
  }

  @GetMapping("/{baseSymbol}")
  @Operation(
      summary = "Get Warrants by Base Symbol",
      description = "Retrieve a list of warrants for a specific base symbol")
  public List<Symbol> getWarrantsBySymbol(@PathVariable String baseSymbol) {
    return warrantService.getWarrantsBySymbol(baseSymbol);
  }

  @PostMapping("/analysis")
  @Operation(summary = "Analyze Warrants", description = "Analyze warrants based on criteria")
  public List<@NonNull AnalysisResponse> analyzeWarrants(
      @Validated @RequestBody AnalysisRequest request,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return warrantService.getAnalysis(request, page, size);
  }
}
