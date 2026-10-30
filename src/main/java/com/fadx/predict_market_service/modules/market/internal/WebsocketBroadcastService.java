package com.fadx.predict_market_service.modules.market.internal;

import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_BIDOFFER;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_QUOTE;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.CHANNEL_BIDOFFER_PREFIX;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.CHANNEL_QUOTE_PREFIX;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.FIELD_PUBLICATION;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.FIELD_SYMBOL;

import com.fadx.predict_market_service.core.api.RealtimePublisher;
import com.fadx.predict_market_service.modules.common.dto.BidOfferQuote;
import com.fadx.predict_market_service.modules.common.dto.MovingAverage;
import com.fadx.predict_market_service.modules.common.dto.PriceVolume;
import com.fadx.predict_market_service.modules.common.dto.QuoteData;
import com.fadx.predict_market_service.modules.common.dto.WarrantAnalysis;
import com.fadx.predict_market_service.modules.common.dto.WarrantSignal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsocketBroadcastService {

  private final RedisService redisService;
  private final ObjectMapper objectMapper;
  private final RealtimePublisher realtimePublisher;

  // =========================================================================
  // KAFKA-DRIVEN PATH (primary): one call per market update from the consumer
  // =========================================================================

  /**
   * Entry point for the Kafka consumer. Call this with the raw JSON payload as soon as a message is
   * consumed, e.g.: {"p": ["market.quote.TCB", {"s":"TCB","c":28250,"ch":0,"vo":5708600,...}]}
   *
   * <p>Does nothing (and stays cheap) if nobody is currently subscribed to the symbol, or if it was
   * just pushed within minPushIntervalMs.
   */
  public void onMarketUpdate(String rawJsonPayload) {
    try {
      JsonNode root = objectMapper.readTree(rawJsonPayload);
      JsonNode pNode = root.get(FIELD_PUBLICATION);
      if (pNode == null || !pNode.isArray() || pNode.size() < 2) {
        return;
      }
      String channel = pNode.get(0).asText("");
      if (channel.isBlank()) {
        return;
      }
      if (channel.startsWith(CHANNEL_QUOTE_PREFIX)) {
        // Pass through the full provider quote object instead of converting it to QuoteData.
        // A stock quote currently includes 18 fields:
        // s, ss, ti, o, h, l, c, ch, r, mv, vo, va, mb, a, ba, frBvo, frSvo, frCr.
        // Index quotes have a different shape (for example t, fsp, noFsp). Keeping p[1] intact
        // prevents newly added provider fields from silently disappearing before reaching the FE.
        JsonNode quote = pNode.get(1);
        String symbol =
            quote != null && quote.hasNonNull(FIELD_SYMBOL)
                ? quote.get(FIELD_SYMBOL).asText()
                : channel.substring(CHANNEL_QUOTE_PREFIX.length());
        handleSingleUpdate(symbol, quote, REALTIME_OUTBOUND_QUOTE);
      } else if (channel.startsWith(CHANNEL_BIDOFFER_PREFIX)) {
        for (BidOfferQuote bidOffer : parseBidOffer(pNode.get(1), channel)) {
          handleSingleUpdate(bidOffer.getS(), bidOffer, REALTIME_OUTBOUND_BIDOFFER);
        }
      } else {
        log.warn("Unknown MAS channel pattern, skipping: {}", channel);
      }
    } catch (JsonProcessingException e) {
      log.error("Error parsing Kafka payload: {}", e.getMessage(), e);
    }
  }

  private List<WarrantAnalysis> parseWarrantAnalysis(JsonNode data) {
    List<WarrantAnalysis> result = new ArrayList<>();
    try {
      // data dạng {"HPG": [ {...}, {...}, ... ]} — chỉ có 1 key là mã cơ sở
      Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        JsonNode warrantArray = entry.getValue();

        if (warrantArray == null || !warrantArray.isArray()) {
          continue;
        }

        for (JsonNode item : warrantArray) {
          WarrantAnalysis w = parseSingleWarrant(item);
          if (w != null && w.getS() != null && !w.getS().isBlank()) {
            result.add(w);
          }
        }
      }
    } catch (Exception e) {
      log.error("Error parsing warrant analysis payload: {}", e.getMessage());
    }
    return result;
  }

  private WarrantAnalysis parseSingleWarrant(JsonNode item) {
    try {
      WarrantAnalysis w = new WarrantAnalysis();
      w.setS(item.hasNonNull("s") ? item.get("s").asText() : null);
      w.setB(item.hasNonNull("b") ? item.get("b").asText() : null);
      w.setUp(readBigDecimal(item, "up"));
      w.setExp(readBigDecimal(item, "exp"));
      w.setEd(item.hasNonNull("ed") ? item.get("ed").asText() : null);
      w.setDtm(item.hasNonNull("dtm") ? item.get("dtm").asInt() : null);
      w.setTimeToMaturity(readBigDecimal(item, "time_to_maturity"));
      w.setExr(readBigDecimal(item, "exr"));
      w.setSt(item.hasNonNull("st") ? item.get("st").asText() : null);
      w.setTp(readBigDecimal(item, "tp"));
      w.setMp(readBigDecimal(item, "mp"));
      w.setBepv(readBigDecimal(item, "bepv"));
      w.setBep(readBigDecimal(item, "bep"));
      w.setVo(readBigDecimal(item, "vo"));
      w.setWarrantScore(readBigDecimal(item, "warrant_score"));
      w.setMomentumScore(readBigDecimal(item, "momentum_score"));
      w.setScore(readBigDecimal(item, "score"));
      w.setRank(item.hasNonNull("rank") ? item.get("rank").asInt() : null);

      JsonNode signalNode = item.get("signal");
      if (signalNode != null && !signalNode.isMissingNode()) {
        WarrantSignal signal = new WarrantSignal();
        signal.setRsi(readBigDecimal(signalNode, "rsi"));
        signal.setRsiLabel(
            signalNode.hasNonNull("rsi_label") ? signalNode.get("rsi_label").asText() : null);
        signal.setMacd(readBigDecimal(signalNode, "macd"));
        signal.setMacdLabel(
            signalNode.hasNonNull("macd_label") ? signalNode.get("macd_label").asText() : null);
        signal.setMaLabel(
            signalNode.hasNonNull("ma_label") ? signalNode.get("ma_label").asText() : null);
        signal.setVol(readBigDecimal(signalNode, "vol"));
        signal.setVolMa20(readBigDecimal(signalNode, "vol_ma20"));
        signal.setVolLabel(
            signalNode.hasNonNull("vol_label") ? signalNode.get("vol_label").asText() : null);

        JsonNode maNode = signalNode.get("ma");
        if (maNode != null && !maNode.isMissingNode()) {
          MovingAverage ma = new MovingAverage();
          ma.setMa20(readBigDecimal(maNode, "ma20"));
          ma.setMa50(readBigDecimal(maNode, "ma50"));
          ma.setMa200(readBigDecimal(maNode, "ma200"));
          signal.setMa(ma);
        }
        w.setSignal(signal);
      }

      return w;
    } catch (Exception e) {
      log.error("Error parsing single warrant item: {}", e.getMessage());
      return null;
    }
  }

  public void handleSingleUpdate(String symbol, Object payload, String topic) {
    realtimePublisher.publish(symbol, payload, topic);
  }

  /**
   * Parses a raw ticker payload into Stock DTO(s).
   *
   * <p>Real shape confirmed from Kafka: {"p": ["market.quote.TCB",
   * {"s":"TCB","ss":"","ti":..,"o":28150,"h":28500,
   * "l":27800,"c":28250,"ch":0,"r":0,"mv":2000,"vo":5708600,"va":..,
   * "mb":"SELL","a":28155,"ba":0,"frBvo":..,"frSvo":..,"frCr":..}]}
   *
   * <p>p[0] is "{topic}.{symbol}" (e.g. "market.quote.TCB") — split explicitly into topic
   * ("market.quote") and symbol ("TCB") instead of treating it as an opaque string. p[1] is the
   * tick data object, kept as-is.
   *
   * <p>Shared by the Redis-backed fetch path and the Kafka-driven push path so there's exactly one
   * place that understands this JSON shape.
   */
  private List<QuoteData> parseStocks(JsonNode data, String topicAndSymbol) {
    List<QuoteData> result = new ArrayList<>();

    try {
      String s = data.hasNonNull("s") ? data.get("s").asText() : topicAndSymbol;
      if (s == null || s.isBlank() || !data.hasNonNull("c")) {
        log.warn("Skipping unparseable quote on topic {}: {}", topicAndSymbol, data);
        return result;
      }

      BigDecimal it = readBigDecimal(data, "ti");
      BigDecimal o = readBigDecimal(data, "o");
      BigDecimal h = readBigDecimal(data, "h");
      BigDecimal l = readBigDecimal(data, "l");
      BigDecimal c = readBigDecimal(data, "c");
      BigDecimal ch = readBigDecimal(data, "ch");
      BigDecimal r = readBigDecimal(data, "r");
      BigDecimal a = readBigDecimal(data, "a");
      long mv = data.hasNonNull("mv") ? data.get("mv").asLong() : 0L;
      long vo = data.hasNonNull("vo") ? data.get("vo").asLong() : 0L;
      long va = data.hasNonNull("va") ? data.get("va").asLong() : 0L;
      BigDecimal frBvo = readBigDecimal(data, "frBvo");
      BigDecimal frSvo = readBigDecimal(data, "frSvo");
      BigDecimal frCr = readBigDecimal(data, "frCr");

      // Payload không có field reference price riêng, suy ra close - change
      // (như trước) — dùng để QuoteData tự tính ce/fl trong constructor.
      BigDecimal rp = c.subtract(ch);

      QuoteData quoteData =
          new QuoteData(s, it, o, h, l, c, ch, r, rp, a, mv, vo, va, frBvo, frSvo, frCr);

      result.add(quoteData);
    } catch (Exception e) {
      log.error("Error parsing quote payload: {}", e.getMessage());
    }

    return result;
  }

  private List<BidOfferQuote> parseBidOffer(JsonNode data, String topicAndSymbol) {
    List<BidOfferQuote> result = new ArrayList<>();
    try {
      String s = data.hasNonNull("s") ? data.get("s").asText() : topicAndSymbol;
      if (s == null || s.isBlank()) {
        log.warn("Skipping unparseable BidOffer on topic {}: {}", topicAndSymbol, data);
        return result;
      }
      BigDecimal ti = readBigDecimal(data, "ti");

      BigDecimal frBvo = readBigDecimal(data, "frBvo");
      BigDecimal frSvo = readBigDecimal(data, "frSvo");
      BigDecimal frBva = readBigDecimal(data, "frBva");
      BigDecimal frSva = readBigDecimal(data, "frSva");
      BigDecimal frTr = readBigDecimal(data, "frTr");
      BigDecimal frCr = readBigDecimal(data, "frCr");

      BigDecimal tbo = readBigDecimal(data, "tbo");
      BigDecimal too = readBigDecimal(data, "too");

      List<PriceVolume> bb = readPriceVolumeList(data, "bb");
      List<PriceVolume> bo = readPriceVolumeList(data, "bo");

      BidOfferQuote bidOfferQuote = new BidOfferQuote();
      bidOfferQuote.setS(s);
      bidOfferQuote.setTi(ti);
      bidOfferQuote.setBb(bb);
      bidOfferQuote.setBo(bo);
      bidOfferQuote.setFrBvo(frBvo);
      bidOfferQuote.setFrSvo(frSvo);
      bidOfferQuote.setFrBva(frBva);
      bidOfferQuote.setFrSva(frSva);
      bidOfferQuote.setFrTr(frTr);
      bidOfferQuote.setFrCr(frCr);
      bidOfferQuote.setTbo(tbo);
      bidOfferQuote.setToo(too);
      result.add(bidOfferQuote);
    } catch (Exception e) {
      log.error("Error parsing quote payload: {}", e.getMessage());
    }

    return result;
  }

  private List<PriceVolume> readPriceVolumeList(JsonNode data, String field) {
    List<PriceVolume> list = new ArrayList<>();
    if (data == null) return list;

    JsonNode arr = data.get(field);
    if (arr == null || !arr.isArray()) {
      return list; // field không tồn tại (ví dụ payload CHPG2616) -> trả về list rỗng thay vì lỗi
    }

    for (JsonNode item : arr) {
      BigDecimal p = readBigDecimal(item, "p");
      BigDecimal v = readBigDecimal(item, "v");
      list.add(new PriceVolume(p, v));
    }
    return list;
  }

  private BigDecimal readBigDecimal(JsonNode data, String field) {
    return data.hasNonNull(field) ? new BigDecimal(data.get(field).asText()) : BigDecimal.ZERO;
  }

  public void onWarrantAnalysis(String rawPayload) {
    try {
      for (WarrantAnalysis warrant : parseWarrantAnalysis(objectMapper.readTree(rawPayload))) {
        handleSingleUpdate(warrant.getS(), warrant, "realtime.outbound.analysis." + warrant.getS());
      }
    } catch (JsonProcessingException e) {
      log.error("Error parsing warrant analysis payload: {}", e.getMessage(), e);
    }
  }
}
