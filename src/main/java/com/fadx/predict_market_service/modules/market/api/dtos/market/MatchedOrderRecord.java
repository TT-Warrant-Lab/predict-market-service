package com.fadx.predict_market_service.modules.market.api.dtos.market;

public record MatchedOrderRecord(
    Long ti,
    Double o,
    Double h,
    Double l,
    Double c,
    Double ch,
    Double r,
    Long mv,
    Long vo,
    Long va,
    String mb) {}
