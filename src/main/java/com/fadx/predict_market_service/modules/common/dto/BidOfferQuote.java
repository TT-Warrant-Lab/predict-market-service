package com.fadx.predict_market_service.modules.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class BidOfferQuote {

  private String s;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal ti;

  private List<PriceVolume> bb;
  private List<PriceVolume> bo;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal frBvo;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal frSvo;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal frBva;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal frSva;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal frTr;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal frCr;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal tbo;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal too;
}
