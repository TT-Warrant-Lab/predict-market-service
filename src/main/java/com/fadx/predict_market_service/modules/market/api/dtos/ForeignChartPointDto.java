package com.fadx.predict_market_service.modules.market.api.dtos;

import java.time.LocalDateTime;

/**
 * 1 điểm dữ liệu trên chart foreign trading trong ngày, đại diện cho 1 phút. Các field buy/sell/net
 * đều là khối lượng PHÁT SINH trong phút đó (đã tính delta từ giá trị cộng dồn foreign_buy_vol /
 * foreign_sell_vol / foreign_net_vol).
 */
public record ForeignChartPointDto(
    LocalDateTime time,
    long buyVolume,
    long sellVolume,
    long netVolume,
    long cumulativeNetVolume // net cộng dồn từ đầu phiên tới hết phút đó, tiện vẽ đường xu hướng
    ) {}
