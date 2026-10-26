package com.fadx.predict_market_service.modules.common.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class QuoteData {
  public String s;
  public BigDecimal it;
  public BigDecimal o;
  public BigDecimal h;
  public BigDecimal l;
  public BigDecimal c;

  public BigDecimal ch;
  public BigDecimal r;

  public BigDecimal rp;
  public BigDecimal ce;
  public BigDecimal fl;
  public BigDecimal a;

  public long mv;
  public long vo;
  public long va;

  public BigDecimal frBvo;
  public BigDecimal frSvo;
  public BigDecimal frCr;

  public QuoteData(String s, BigDecimal rp) {
    this.s = s;
    this.rp = rp;
    this.c = rp;
    this.ce = round(rp.multiply(new BigDecimal("1.07")));
    this.fl = round(rp.multiply(new BigDecimal("0.93")));
    this.vo = 0;
  }

  public QuoteData(
      String s,
      BigDecimal it,
      BigDecimal o,
      BigDecimal h,
      BigDecimal l,
      BigDecimal c,
      BigDecimal ch,
      BigDecimal r,
      BigDecimal rp,
      BigDecimal a,
      long mv,
      long vo,
      long va,
      BigDecimal frBvo,
      BigDecimal frSvo,
      BigDecimal frCr) {
    this.s = s;
    this.it = it;
    this.o = o;
    this.h = h;
    this.l = l;
    this.c = c;
    this.ce = round(rp.multiply(new BigDecimal("1.07")));
    this.fl = round(rp.multiply(new BigDecimal("0.93")));
    this.ch = ch;
    this.r = r;
    this.rp = rp;
    this.a = a;
    this.mv = mv;
    this.vo = vo;
    this.va = va;
    this.frBvo = frBvo;
    this.frSvo = frSvo;
    this.frCr = frCr;
  }

  private BigDecimal round(BigDecimal v) {
    return v.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).multiply(BigDecimal.TEN);
  }
}
