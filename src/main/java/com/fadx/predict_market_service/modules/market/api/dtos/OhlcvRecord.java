package com.fadx.predict_market_service.modules.market.api.dtos;

import java.util.List;

public record OhlcvRecord(
    List<Double> t,
    List<Double> o,
    List<Double> h,
    List<Double> l,
    List<Double> c,
    List<Double> v,
    String s,
    Long nt) {}
