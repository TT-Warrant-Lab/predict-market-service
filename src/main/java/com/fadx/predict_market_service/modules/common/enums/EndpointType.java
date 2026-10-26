package com.fadx.predict_market_service.modules.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EndpointType {
  STOCK("stock"),
  MARKET("market"),
  TRADING("trading"),
  FOREIGN("foreign"),
  STATIC_DATA("static_data"),
  MEDIA("media");

  private final String value;
}
