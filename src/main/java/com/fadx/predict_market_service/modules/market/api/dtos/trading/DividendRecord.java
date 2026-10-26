package com.fadx.predict_market_service.modules.market.api.dtos.trading;

public record DividendRecord(
    String s,
    String ed,
    String rd,
    String pd,
    String dt,
    Double cv,
    Double sr,
    Double st,
    Integer y,
    String p,
    String n,
    String status) {}
