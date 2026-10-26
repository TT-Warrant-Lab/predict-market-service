package com.fadx.predict_market_service.modules.common.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WarrantAnalysis {
  private String s; // mã warrant, vd: CHPG2612
  private String b; // mã cổ phiếu cơ sở, vd: HPG
  private BigDecimal up; // underlying price
  private BigDecimal exp; // giá kỳ vọng / exercise price
  private String ed; // ngày đáo hạn dd/MM/yyyy
  private Integer dtm; // days to maturity
  private BigDecimal timeToMaturity;
  private BigDecimal exr; // exercise ratio
  private String st; // status: ITM / OTM
  private BigDecimal tp; // theoretical price
  private BigDecimal mp; // market price
  private BigDecimal bepv; // break-even value
  private BigDecimal bep; // break-even %
  private BigDecimal vo; // volatility
  private BigDecimal warrantScore;
  private BigDecimal momentumScore;
  private BigDecimal score;
  private Integer rank;
  private WarrantSignal signal;
}
