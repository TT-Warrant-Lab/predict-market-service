package com.fadx.predict_market_service.modules.market.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/** Service class để thao tác với Redis sử dụng JedisPool */
@Slf4j
@Service
public class RedisService {

  @Autowired private JedisPool jedisPool;

  /** Set giá trị trong Redis (không expiry) */
  public void set(String key, String value) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.set(key, value);
    } catch (Exception e) {
      log.error("Error setting key: {}", key, e);
      throw e;
    }
  }

  /** Set giá trị với thời gian sống (TTL) */
  public void setWithExpiry(String key, String value, int seconds) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.setex(key, seconds, value);
    } catch (Exception e) {
      log.error("Error setting key with expiry: {}", key, e);
      throw e;
    }
  }

  /** Get giá trị từ Redis */
  public String get(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      String value = jedis.get(key);
      log.info("Get key: {} = {}", key, value);
      return value;
    } catch (Exception e) {
      log.error("Error getting key: {}", key, e);
      throw e;
    }
  }

  /** Delete một key */
  public void delete(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(key);
    } catch (Exception e) {
      log.error("Error deleting key: {}", key, e);
      throw e;
    }
  }

  /** Check key tồn tại */
  public boolean exists(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      boolean exists = jedis.exists(key);
      return exists;
    } catch (Exception e) {
      log.error("Error checking key existence: {}", key, e);
      throw e;
    }
  }

  /** Increment giá trị (cho Integer) */
  public long increment(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      long value = jedis.incr(key);
      return value;
    } catch (Exception e) {
      log.error("Error incrementing key: {}", key, e);
      throw e;
    }
  }

  /** Push giá trị vào List */
  public void lpush(String key, String... values) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.lpush(key, values);
    } catch (Exception e) {
      log.error("Error pushing to list: {}", key, e);
      throw e;
    }
  }

  /** Pop giá trị từ List */
  public String lpop(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      String value = jedis.lpop(key);
      return value;
    } catch (Exception e) {
      log.error("Error popping from list: {}", key, e);
      throw e;
    }
  }

  /** Get Redis connection pool info */
  public String getPoolInfo() {
    try (Jedis jedis = jedisPool.getResource()) {
      String info = jedis.info("stats");
      return info;
    } catch (Exception e) {
      log.error("Error getting pool info", e);
      throw e;
    }
  }

  // =========================================================================
  // KHÔNG GIAN BỔ SUNG: THAO TÁC VỚI SORTED SET (ZSET) & HASH (HSET)
  // =========================================================================

  /**
   * Thêm một phần tử vào Sorted Set (ZSET) kèm theo Score (Timestamp) Thích hợp lưu lịch sử biến
   * động/chuỗi thời gian.
   */
  public void zadd(String key, double score, String member) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.zadd(key, score, member);
    } catch (Exception e) {
      log.error("Error executing ZADD on key: {}", key, e);
      throw e;
    }
  }

  /**
   * Lấy toàn bộ dữ liệu từ Sorted Set (ZSET) dựa trên khoảng Score (Sắp xếp tăng dần) Thích hợp bóc
   * tách chuỗi thời gian từ đầu phiên đến cuối phiên để vẽ biểu đồ Line.
   */
  public List<redis.clients.jedis.resps.Tuple> zrangeByScoreWithScores(
      String key, double min, double max) {
    try (Jedis jedis = jedisPool.getResource()) {
      List<redis.clients.jedis.resps.Tuple> result = jedis.zrangeByScoreWithScores(key, min, max);
      return result;
    } catch (Exception e) {
      log.error("Error executing ZRANGEBYSCORE on key: {}", key, e);
      throw e;
    }
  }

  /**
   * Set một trường (field) trong bảng Hash (HSET) Thích hợp lưu trữ bảng điện rút gọn (Trạng thái
   * Latest của một mã)
   */
  public void hset(String key, String field, String value) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.hset(key, field, value);
    } catch (Exception e) {
      log.error("Error executing HSET on key: {}, field: {}", key, field, e);
      throw e;
    }
  }

  /** Set nhiều field trong một Redis hash bằng một kết nối. */
  public void hset(String key, Map<String, String> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.hset(key, values);
    } catch (Exception e) {
      log.error("Error executing HSET on key: {}", key, e);
      throw e;
    }
  }

  /**
   * Lấy giá trị của nhiều trường (fields) cùng một lúc trong bảng Hash (HMGET) Giúp lấy thông tin
   * LATEST của một danh sách mã chứng khoán chỉ với 1 request.
   */
  public List<String> hmget(String key, String... fields) {
    try (Jedis jedis = jedisPool.getResource()) {
      List<String> values = jedis.hmget(key, fields);
      return values;
    } catch (Exception e) {
      log.error("Error executing HMGET on key: {}", key, e);
      throw e;
    }
  }

  /**
   * Kiểm tra và đặt thời gian sống (TTL) cho một key nếu key đó CHƯA ĐƯỢC SET TTL. Tránh việc lặp
   * lại lệnh EXPIRE lãng phí tài nguyên khi stream đổ về liên tục.
   */
  public void expireIfAbsent(String key, int seconds) {
    try (Jedis jedis = jedisPool.getResource()) {
      if (jedis.ttl(key) == -1) {
        jedis.expire(key, seconds);
      }
    } catch (Exception e) {
      log.error("Error setting EXPIRE on key: {}", key, e);
      throw e;
    }
  }

  public Set<String> keys(String pattern) {
    try (redis.clients.jedis.Jedis jedis =
        jedisPool.getResource()) { // Hoặc tùy cách bạn quản lý kết nối Redis
      return jedis.keys(pattern);
    } catch (Exception e) {
      log.error("Lỗi khi quét danh sách keys với pattern: {}", pattern, e);
      return Collections.emptySet();
    }
  }

  public Map<String, String> hgetAll(String hashKey) {
    try (Jedis jedis = jedisPool.getResource()) {
      Map<String, String> result = jedis.hgetAll(hashKey);
      return result;
    } catch (Exception e) {
      log.error("Error executing HGETALL on key: {}", hashKey, e);
      return Collections.emptyMap();
    }
  }
}
