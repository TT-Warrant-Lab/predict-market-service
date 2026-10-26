package com.fadx.predict_market_service.modules.common.dto;

import java.time.LocalDateTime;

/**
 * 1 điểm dữ liệu trên chart intraday của ticker, đại diện cho 1 phút giao dịch.
 *
 * @param time mốc thời gian đã làm tròn xuống phút (vd 09:15:00)
 * @param price giá khớp cuối cùng trong phút đó (last price)
 * @param volume khối lượng khớp TRONG phút đó (đã tính delta từ accumulated_volume, không phải khối
 *     lượng cộng dồn)
 * @param cumulativeVolume khối lượng cộng dồn từ đầu phiên tính đến hết phút đó (tham khảo thêm)
 */
public record TickerChartPointDto(
    LocalDateTime time, Double price, long volume, long cumulativeVolume) {}
