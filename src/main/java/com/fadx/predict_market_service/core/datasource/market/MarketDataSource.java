package com.fadx.predict_market_service.core.datasource.market;

import com.fadx.predict_market_service.modules.market.api.dtos.market.MatchedOrderPageRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.QuoteSummaryItemRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolCandleRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolQuoteRecord;
import java.util.List;

/** Market domain contract. Controllers deliberately know nothing about MAS or HTTP paths. */
public interface MarketDataSource {
  List<SymbolQuoteRecord> currentPrices(List<String> symbols);

  MatchedOrderPageRecord quote(String symbol, int fetchCount, Integer lastIndex, Integer lastSize);

  List<QuoteSummaryItemRecord> quoteSummary(String symbol);

  List<SymbolCandleRecord> history(String symbol, String fromDate, String toDate, int limit);
}
