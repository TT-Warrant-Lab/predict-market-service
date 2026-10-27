package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.modules.market.api.dtos.MarketStatusResponse;
import com.fadx.predict_market_service.modules.market.internal.IndexTopMoversService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata/static")
@RequiredArgsConstructor
@Tag(name = "Anonymous | Static Data", description = "Static data")
public class StaticDataController {

  private final IndexTopMoversService indexTopMoversService;

  @org.springframework.beans.factory.annotation.Value("${app.static-data.sync-dir:data/static}")
  private String syncDirectory;

  private static final String INDEX_STOCK_LIST_PATH = "static/indexStockList.json";
  private static final String SYMBOL_STATIC_DATA_PATH = "static/symbol_static_data.json";
  private static final String WARRANT_STATIC_DATA_PATH = "static/warrant_static_data.json";
  private static final String SYMBOL_HAVE_WARRANT_LIST_PATH =
      "static/symbols_have_warrant_list.json";

  @GetMapping("/indexStockList.json")
  public ResponseEntity<Resource> getIndexStockList() {
    return loadJsonResource(INDEX_STOCK_LIST_PATH);
  }

  @GetMapping("/top-movers")
  public ResponseEntity<List<Map<String, Object>>> getTopMovers() {
    return ResponseEntity.ok(indexTopMoversService.getTopMovers());
  }

  @GetMapping("/market-status")
  public ResponseEntity<MarketStatusResponse> getMarketStatus() {
    return ResponseEntity.ok(indexTopMoversService.getMarketStatus());
  }

  @GetMapping("/symbol_static_data.json")
  public ResponseEntity<Resource> getSymbolStaticData() {
    return loadJsonResource(SYMBOL_STATIC_DATA_PATH);
  }

  @GetMapping("/warrant_static_data.json")
  public ResponseEntity<Resource> getWarrantStaticData() {
    return loadJsonResource(WARRANT_STATIC_DATA_PATH);
  }

  @GetMapping("/symbols_have_warrant_list.json")
  public ResponseEntity<Resource> getSymbolsHaveWarrantData() {
    return loadJsonResource(SYMBOL_HAVE_WARRANT_LIST_PATH);
  }

  private ResponseEntity<Resource> loadJsonResource(String classpathLocation) {
    try {
      Path external = Path.of(syncDirectory, Path.of(classpathLocation).getFileName().toString());
      if (Files.exists(external)) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .body(new org.springframework.core.io.FileSystemResource(external));
      }
      Resource resource = new ClassPathResource(classpathLocation);
      if (!resource.exists()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.CACHE_CONTROL, "no-cache")
          .body(resource);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
