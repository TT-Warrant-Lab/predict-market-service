package com.fadx.predict_market_service.modules.market.api.dtos.market;

import com.fadx.predict_market_service.modules.market.api.dtos.IndexChangeRecord;
import java.util.List;

public record SymbolQuoteRecord(
    Double o,
    Double h,
    Double l,
    Double c,
    Double ch,
    Double r,
    Long vo,
    Long va,
    Double a,
    String ti,
    PriceVolumeRecord hm,
    PriceVolumeRecord lm,
    Long frBvo,
    Long frBva,
    Long frSvo,
    Long frSva,
    String s,
    String ss,
    List<PriceVolumeRecord> bb,
    List<PriceVolumeRecord> bo,
    List<PriceVolumeRecord> bbOd,
    List<PriceVolumeRecord> boOd,
    Long tbo,
    Long too,
    Long mbo,
    Long mso,
    Long frTr,
    Long av10,
    Long av20,
    Double odC,
    Double odCh,
    Long odVo,
    IndexChangeRecord ic) {}
