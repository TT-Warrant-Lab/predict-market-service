package com.fadx.predict_market_service.core.api;

/**
 * Outbound realtime delivery port. Domain modules publish a topic payload without depending on the
 * WebSocket transport.
 */
public interface RealtimePublisher {

  void publish(String symbol, Object payload, String topic);
}
