package com.fadx.predict_market_service.modules.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Trading-day offset relative to the latest matching-history date in storage. */
@Getter
@RequiredArgsConstructor
public enum MatchHistoryDay {
  T(0),
  T_MINUS_1(1),
  T_MINUS_2(2),
  T_MINUS_3(3),
  T_MINUS_4(4),
  T_MINUS_5(5);

  private final int offset;
}
