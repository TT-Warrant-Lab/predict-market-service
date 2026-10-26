package com.fadx.predict_market_service.modules.market.api.dtos.stock.company;

public record CompanyProfileRecord(
    Double h52w,
    Double l52w,
    Double pch52w,
    Long mc,
    Double eps,
    Double pe,
    Double pb,
    Double beta,
    Double div,
    Double yld,
    Long sl,
    Long si,
    Double ff,
    Long tr,
    Long cr,
    Double oar,
    String ss,
    String sub) {}
