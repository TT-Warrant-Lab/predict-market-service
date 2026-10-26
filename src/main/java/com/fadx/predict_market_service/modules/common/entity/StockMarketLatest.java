package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "t_stock_market_latest",
    indexes = {
      @Index(name = "idx_market_latest_lookup", columnList = "ticker, trading_date", unique = true),
      @Index(name = "idx_market_latest_market_date", columnList = "market, trading_date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMarketLatest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String ticker; // Mã chứng khoán hoặc Chứng quyền (VRE, TCB, CVHM2520...)

  @Column(name = "trading_date", nullable = false)
  private LocalDate tradingDate; // Ngày giao dịch (yyyy-MM-dd)

  @Column(length = 20)
  private String market;

  @Column(name = "index_memberships", length = 100)
  private String indexMemberships;

  @Column(name = "close_price")
  private Double closePrice; // Giá đóng cửa cuối phiên (Trường 'c' từ JSON gốc)

  @Column(name = "total_volume")
  private Long totalVolume; // Tổng khối lượng khớp lệnh cả ngày (Trường 'vo')

  @Column(name = "foreign_buy_vol")
  private Long foreignBuyVol; // Khối lượng ngoại mua cả ngày (Trường 'frBvo')

  @Column(name = "foreign_sell_vol")
  private Long foreignSellVol; // Khối lượng ngoại bán cả ngày (Trường 'frSvo')

  @Column(name = "foreign_net_vol")
  private Long foreignNetVol; // Khối lượng ròng ngoại (Buy - Sell)

  @Column(name = "raw_json_snapshot", columnDefinition = "TEXT")
  private String
      rawJsonSnapshot; // Lưu trọn vẹn JSON thô từ Redis để bóc tách thêm nếu MAS update trường mới

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
