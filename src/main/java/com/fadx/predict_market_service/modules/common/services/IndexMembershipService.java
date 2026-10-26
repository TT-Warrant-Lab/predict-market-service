package com.fadx.predict_market_service.modules.common.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Resolves a symbol's index memberships from the static index constituent file. */
@Service
@RequiredArgsConstructor
public class IndexMembershipService {

  private static final String INDEX_STOCK_LIST_RESOURCE = "static/indexStockList.json";

  private final ObjectMapper objectMapper;
  private Map<String, String> membershipsBySymbol = Map.of();
  private Map<String, java.util.List<String>> symbolsByIndex = Map.of();

  @PostConstruct
  void loadMemberships() {
    try {
      Map<String, Set<String>> indexesBySymbol = new LinkedHashMap<>();
      Map<String, java.util.List<String>> configuredIndices =
          objectMapper.readValue(
              new ClassPathResource(INDEX_STOCK_LIST_RESOURCE).getInputStream(),
              new TypeReference<LinkedHashMap<String, java.util.List<String>>>() {});
      Map<String, java.util.List<String>> normalizedSymbolsByIndex = new LinkedHashMap<>();
      configuredIndices.forEach(
          (index, symbols) -> {
            String normalizedIndex = normalize(index);
            java.util.List<String> normalizedSymbols =
                symbols.stream().map(this::normalize).distinct().sorted().toList();
            normalizedSymbolsByIndex.put(normalizedIndex, normalizedSymbols);
            normalizedSymbols.forEach(
                symbol ->
                    indexesBySymbol
                        .computeIfAbsent(symbol, ignored -> new java.util.TreeSet<>())
                        .add(normalizedIndex));
          });

      Map<String, String> resolvedMemberships = new LinkedHashMap<>();
      indexesBySymbol.forEach(
          (symbol, indexes) -> resolvedMemberships.put(symbol, String.join(",", indexes)));
      membershipsBySymbol = Map.copyOf(resolvedMemberships);
      symbolsByIndex = Map.copyOf(normalizedSymbolsByIndex);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot load " + INDEX_STOCK_LIST_RESOURCE + " for index memberships", exception);
    }
  }

  public String getIndexMemberships(String ticker) {
    return membershipsBySymbol.getOrDefault(normalize(ticker), "");
  }

  public java.util.List<String> getSymbolsForIndex(String index) {
    return symbolsByIndex.getOrDefault(normalize(index), java.util.List.of());
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
