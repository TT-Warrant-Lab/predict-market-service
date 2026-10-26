package com.fadx.predict_market_service.modules.market.api.dtos.market;

import java.util.List;

public record MatchedOrderPageRecord(
    Long lastIndex, Long lastSize, List<MatchedOrderRecord> data) {}
