package com.fadx.predict_market_service.core.messaging;

import com.fadx.predict_market_service.core.config.MasRealtimeProperties;
import com.fadx.predict_market_service.modules.symbol.internal.entity.Symbol;
import com.fadx.predict_market_service.modules.symbol.internal.repositories.SymbolRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Direct, single-connection MAS feed owned by predict-market-service.
 *
 * <p>Preserves the prior subscriber's important behaviour: Vietnam-market schedule, SocketCluster
 * handshake, bounded subscription batches, #1/#2 heartbeat, MessagePack frames, and exponential
 * reconnect. Market projections remain consumers of the Kafka topics published here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MasDirectSubscriber {
  private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final LocalTime OPEN = LocalTime.of(8, 58);
  private static final LocalTime CLOSE = LocalTime.of(15, 5);
  private static final String QUOTE_PREFIX = "market.quote.";
  private static final String BIDOFFER_PREFIX = "market.bidoffer.";

  private final MasRealtimeProperties properties;
  private final SymbolRepository symbolRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "mas-feed-scheduler"));
  private final ExecutorService socketExecutor =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "mas-feed-client"));
  private final HttpClient httpClient = HttpClient.newBuilder().executor(socketExecutor).build();
  private final Object lifecycleLock = new Object();
  private volatile boolean desired;
  private volatile boolean connecting;
  private volatile WebSocket socket;
  private volatile long reconnectDelayMs;
  private volatile int requestID;
  private volatile Set<String> prioritySymbols;

  @PostConstruct
  void initialize() {
    reconnectDelayMs = properties.getReconnectInitialDelayMs();
    scheduler.scheduleWithFixedDelay(this::ensureConnection, 0, 5, TimeUnit.SECONDS);
  }

  @PreDestroy
  void shutdown() {
    desired = false;
    WebSocket current = socket;
    if (current != null) current.sendClose(WebSocket.NORMAL_CLOSURE, "market window closed");
    scheduler.shutdownNow();
    socketExecutor.shutdownNow();
  }

  private void ensureConnection() {
    boolean shouldRun = properties.isEnabled() && inMarketWindow();
    desired = shouldRun;
    if (!shouldRun) {
      WebSocket current = socket;
      if (current != null) current.sendClose(WebSocket.NORMAL_CLOSURE, "outside market window");
      socket = null;
      return;
    }
    if (socket == null && !connecting) connect();
  }

  private boolean inMarketWindow() {
    ZonedDateTime now = ZonedDateTime.now(VIETNAM);
    if (now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY)
      return false;
    LocalTime time = now.toLocalTime();
    return !time.isBefore(OPEN) && time.isBefore(CLOSE);
  }

  private void connect() {
    synchronized (lifecycleLock) {
      if (connecting || socket != null || !desired) return;
      connecting = true;
    }
    try {
      var builder =
          httpClient.newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(15));
      if (!properties.getCookie().isBlank()) builder.header("Cookie", properties.getCookie());
      builder
          .buildAsync(URI.create(properties.getWebsocketUrl()), new FeedListener())
          .whenComplete(
              (ws, error) -> {
                connecting = false;
                if (error != null) reconnect("connect failed: " + error.getMessage());
              });
    } catch (RuntimeException error) {
      connecting = false;
      reconnect("invalid websocket configuration: " + error.getMessage());
    }
  }

  private void reconnect(String reason) {
    socket = null;
    if (!desired) return;
    long delay = reconnectDelayMs;
    reconnectDelayMs = Math.min(reconnectDelayMs * 2, properties.getReconnectMaxDelayMs());
    log.warn("MAS feed disconnected ({}); reconnecting in {} ms", reason, delay);
    scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
  }

  private void onOpen(WebSocket webSocket) {
    socket = webSocket;
    reconnectDelayMs = properties.getReconnectInitialDelayMs();
    requestID = 0;
    Map<String, Object> handshake = new LinkedHashMap<>();
    handshake.put(
        "authToken", properties.getAuthToken().isBlank() ? null : properties.getAuthToken());
    sendEvent("#handshake", handshake);
    scheduler.schedule(() -> subscribeAll(webSocket), 500, TimeUnit.MILLISECONDS);
  }

  private void subscribeAll(WebSocket expectedSocket) {
    if (socket != expectedSocket || !desired) return;
    sendEvent("#subscribe", Map.of("channel", "market.status", "batch", true));
    sendEvent("#subscribe", Map.of("channel", "market.init", "batch", true));
    List<String> channels = subscriptionChannels();
    int batchSize = Math.max(1, properties.getSubscriptionBatchSize());
    for (int start = 0; start < channels.size(); start += batchSize) {
      for (String channel : channels.subList(start, Math.min(start + batchSize, channels.size()))) {
        sendEvent("#subscribe", Map.of("channel", channel));
      }
    }
    log.info("MAS feed subscribed to {} channels", channels.size());
  }

  private List<String> subscriptionChannels() {
    LinkedHashSet<String> symbols = new LinkedHashSet<>();
    symbols.addAll(prioritySymbols());
    // MAS channel casing is significant for these two index names.
    symbols.remove("HNXINDEX");
    symbols.remove("HNXUPCOMINDEX");
    symbols.add("HNXIndex");
    symbols.add("HNXUpcomIndex");
    try {
      for (Symbol symbol : symbolRepository.findAll()) {
        if (symbol.getSymbol() != null && !symbol.getSymbol().isBlank())
          symbols.add(symbol.getSymbol().trim().toUpperCase());
      }
    } catch (Exception error) {
      log.warn("Could not load symbols for MAS subscription; using indexes only", error);
    }
    int maximum = properties.getMaxSymbols();
    List<String> selected = new ArrayList<>(symbols);
    if (maximum > 0 && selected.size() > maximum) selected = selected.subList(0, maximum);
    List<String> result = new ArrayList<>(selected.size() * 2);
    for (String symbol : selected) {
      result.add(QUOTE_PREFIX + symbol);
      result.add(BIDOFFER_PREFIX + symbol);
    }
    return result;
  }

  private void sendEvent(String eventName, Map<String, Object> payload) {
    WebSocket current = socket;
    if (current == null) return;
    Map<String, Object> event = Map.of("e", List.of(eventName, payload, ++requestID));
    try {
      current.sendBinary(ByteBuffer.wrap(pack(event)), true);
    } catch (Exception error) {
      reconnect("send failed: " + error.getMessage());
    }
  }

  private void onFrame(byte[] frame) {
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(frame)) {
      Object payload = unpack(unpacker.unpackValue());
      if (!(payload instanceof Map<?, ?> root)
          || !(root.get("p") instanceof List<?> publication)
          || publication.size() < 2) return;
      if (!(publication.get(0) instanceof String channel)
          || !(publication.get(1) instanceof Map<?, ?>)) return;
      String topic =
          channel.startsWith(BIDOFFER_PREFIX)
              ? "market.bidoffer"
              : channel.startsWith(QUOTE_PREFIX) ? "market.quote" : null;
      if (topic == null) return;
      Object symbolValue = ((Map<?, ?>) publication.get(1)).get("s");
      String symbol =
          symbolValue == null
              ? channel.substring(channel.lastIndexOf('.') + 1)
              : String.valueOf(symbolValue);
      kafkaTemplate
          .send(topic, symbol, payload)
          .whenComplete(
              (result, error) -> {
                if (error != null)
                  log.warn("MAS Kafka publish failed topic={} symbol={}", topic, symbol, error);
              });
      // Priority consumers receive a
      // symbol-specific topic while normal consumers continue to receive market.quote.
      if ("market.quote".equals(topic)
          && prioritySymbols().contains(symbol.toUpperCase(Locale.ROOT))) {
        kafkaTemplate
            .send("market.quote." + symbol, symbol, payload)
            .whenComplete(
                (result, error) -> {
                  if (error != null)
                    log.warn("MAS priority Kafka publish failed symbol={}", symbol, error);
                });
      }
      trace("KAFKA-QUEUED topic=" + topic + " symbol=" + symbol);
    } catch (Exception error) {
      log.warn("Ignoring malformed MAS MessagePack frame", error);
    }
  }

  private void trace(String message) {
    if (properties.isTraceEnabled()) log.info("[MAS-WS] {}", message);
  }

  private Set<String> prioritySymbols() {
    Set<String> cached = prioritySymbols;
    if (cached != null) return cached;
    LinkedHashSet<String> symbols =
        new LinkedHashSet<>(List.of("VN-INDEX", "VN30", "HNXINDEX", "HNXUPCOMINDEX"));
    try (InputStream input = getClass().getResourceAsStream("/static/indexStockList.json")) {
      if (input != null) {
        Map<String, List<String>> indexes = objectMapper.readValue(input, new TypeReference<>() {});
        for (String index : List.of("VN30", "VN100")) {
          for (String symbol : indexes.getOrDefault(index, List.of()))
            symbols.add(symbol.toUpperCase(Locale.ROOT));
        }
      }
    } catch (Exception error) {
      log.warn("Could not load VN30/VN100 index membership; using canonical indexes only", error);
    }
    prioritySymbols = Set.copyOf(symbols);
    return prioritySymbols;
  }

  private static byte[] pack(Object value) throws java.io.IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (MessagePacker packer = MessagePack.newDefaultPacker(output)) {
      packValue(packer, value);
    }
    return output.toByteArray();
  }

  @SuppressWarnings("unchecked")
  private static void packValue(MessagePacker p, Object value) throws java.io.IOException {
    if (value == null) p.packNil();
    else if (value instanceof String v) p.packString(v);
    else if (value instanceof Boolean v) p.packBoolean(v);
    else if (value instanceof Integer v) p.packInt(v);
    else if (value instanceof Long v) p.packLong(v);
    else if (value instanceof Number v) p.packDouble(v.doubleValue());
    else if (value instanceof Map<?, ?> map) {
      p.packMapHeader(map.size());
      for (var entry : map.entrySet()) {
        packValue(p, String.valueOf(entry.getKey()));
        packValue(p, entry.getValue());
      }
    } else if (value instanceof Collection<?> list) {
      p.packArrayHeader(list.size());
      for (Object item : list) packValue(p, item);
    } else throw new IllegalArgumentException("Unsupported MessagePack type: " + value.getClass());
  }

  private static Object unpack(Value value) {
    if (value.isNilValue()) return null;
    if (value.isStringValue()) return value.asStringValue().asString();
    if (value.isBooleanValue()) return value.asBooleanValue().getBoolean();
    if (value.isIntegerValue()) return value.asIntegerValue().asLong();
    if (value.isFloatValue()) return value.asFloatValue().toDouble();
    if (value.isBinaryValue()) return value.asBinaryValue().asByteArray();
    if (value.isArrayValue()) {
      List<Object> items = new ArrayList<>();
      for (Value item : value.asArrayValue()) items.add(unpack(item));
      return items;
    }
    if (value.isMapValue()) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (Map.Entry<Value, Value> entry : value.asMapValue().entrySet())
        map.put(String.valueOf(unpack(entry.getKey())), unpack(entry.getValue()));
      return map;
    }
    return null;
  }

  private final class FeedListener implements WebSocket.Listener {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public void onOpen(WebSocket webSocket) {
      WebSocket.Listener.super.onOpen(webSocket);
      MasDirectSubscriber.this.onOpen(webSocket);
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
      byte[] chunk = new byte[data.remaining()];
      data.get(chunk);
      buffer.writeBytes(chunk);
      if (last) {
        byte[] frame = buffer.toByteArray();
        buffer.reset();
        socketExecutor.execute(() -> onFrame(frame));
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      if (last && "#1".contentEquals(data)) webSocket.sendText("#2", true);
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      reconnect("close " + statusCode + ": " + reason);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      reconnect("socket error: " + error.getMessage());
    }
  }
}
