package com.fadx.predict_market_service.modules.market.api.dtos.market;

public record QuoteSummaryItemRecord(
    Double price,
    Long volume,
    Long buyVolume,
    Long sellVolume,
    Long bsVolume,
    Double buyRate,
    Double sellRate,
    Double bsRate,
    Double rate) {}
