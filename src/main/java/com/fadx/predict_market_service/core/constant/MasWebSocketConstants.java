package com.fadx.predict_market_service.core.constant;

public final class MasWebSocketConstants {
  public static final String EVENT_HANDSHAKE = "#handshake";
  public static final String EVENT_SUBSCRIBE = "#subscribe";
  public static final String EVENT_UNSUBSCRIBE = "#unsubscribe";

  public static final String FIELD_AUTH_TOKEN = "authToken";
  public static final String FIELD_BATCH = "batch";
  public static final String FIELD_CHANNEL = "channel";
  public static final String FIELD_EVENT = "e";
  public static final String FIELD_PUBLICATION = "p";
  public static final String FIELD_SYMBOL = "s";

  public static final String CHANNEL_MARKET_STATUS = "market.status";
  public static final String CHANNEL_MARKET_INIT = "market.init";
  public static final String CHANNEL_QUOTE = "market.quote";
  public static final String CHANNEL_BIDOFFER = "market.bidoffer";
  public static final String CHANNEL_QUOTE_PREFIX = CHANNEL_QUOTE + ".";
  public static final String CHANNEL_BIDOFFER_PREFIX = CHANNEL_BIDOFFER + ".";

  public static final String HEARTBEAT_PING = "#1";
  public static final String HEARTBEAT_PONG = "#2";

  private MasWebSocketConstants() {}
}
