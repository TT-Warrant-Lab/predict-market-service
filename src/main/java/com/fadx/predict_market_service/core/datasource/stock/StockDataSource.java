package com.fadx.predict_market_service.core.datasource.stock;

import com.fadx.predict_market_service.modules.market.api.dtos.stock.ForeignHistoryRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyEventRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyNewsRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyOverviewRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.stock.company.CompanyProfileRecord;
import java.util.List;

public interface StockDataSource {
  List<ForeignHistoryRecord> foreignHistory(
      String symbol, String from, String to, String lastDate, int limit);

  CompanyOverviewRecord overview(String symbol);

  CompanyProfileRecord profile(String symbol);

  List<CompanyEventRecord> events(String symbol, int limit, int language);

  List<CompanyNewsRecord> news(String symbol, int limit, int language);
}
