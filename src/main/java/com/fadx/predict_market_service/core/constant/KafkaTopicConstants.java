package com.fadx.predict_market_service.core.constant;

public final class KafkaTopicConstants {
  public static final String ANALYSIS_WARRANT = "analysis.warrant";
  public static final String REALTIME_OUTBOUND_BIDOFFER = "market.bidoffer";
  public static final String REALTIME_OUTBOUND_QUOTE = "market.quote";
  public static final String REALTIME_OUTBOUND_QUOTE_PRIORITY = "market.quote.priority";
  public static final String REALTIME_OUTBOUND_TREND = "market.trend";
  public static final String REALTIME_OUTBOUND_WARRANT = "analysis.warrant";
  public static final String MARKET_TREND_FOREIGN = "market.trend.foreign";
  public static final String MARKET_TREND_TICKER = "market.trend.ticker";
  public static final String MARKET_ANALYSIS = "market.analysis";
  public static final String INDEX_IMPACT = "index-impact";
  public static final String SIGNALS = "signals";
  public static final String USER_NOTIFICATION = "user.notification";

  private KafkaTopicConstants() {}
}
