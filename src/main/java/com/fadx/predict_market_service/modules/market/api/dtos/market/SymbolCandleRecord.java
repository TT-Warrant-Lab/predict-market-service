package com.fadx.predict_market_service.modules.market.api.dtos.market;

public record SymbolCandleRecord(
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
    Double re,
    Double adj,
    Long pvo,
    Long pva) {}
