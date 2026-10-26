package com.fadx.predict_market_service.modules.market.api.dtos.stock.company;

import java.util.List;

public record CompanyOverviewRecord(
    String fn,
    String ad,
    String ph,
    String fx,
    String em,
    String url,
    String ind,
    String sn,
    Integer ne,
    Long cc,
    String intr,
    List<BoardMemberRecord> bom,
    List<OwnershipRecord> own,
    List<ShareholderRecord> sh) {}
