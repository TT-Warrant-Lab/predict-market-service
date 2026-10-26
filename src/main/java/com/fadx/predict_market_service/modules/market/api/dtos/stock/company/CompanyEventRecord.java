package com.fadx.predict_market_service.modules.market.api.dtos.stock.company;

public record CompanyEventRecord(
    Long id,
    String code,
    String title,
    Integer type,
    String d,
    String ed,
    String rd,
    String rd2,
    String t,
    String c,
    String n,
    String f) {}
