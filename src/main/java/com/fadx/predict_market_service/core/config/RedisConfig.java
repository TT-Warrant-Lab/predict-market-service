package com.fadx.predict_market_service.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/** Spring Boot Configuration cho JedisPool */
@Configuration
public class RedisConfig {

  @Value("${redis.host}")
  private String redisHost;

  @Value("${redis.port}")
  private int redisPort;

  @Value("${redis.password}")
  private String redisPassword;

  @Value("${redis.database}")
  private int database;

  @Value("${redis.timeout}")
  private int timeout;

  @Bean
  public JedisPoolConfig jedisPoolConfig() {
    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    jedisPoolConfig.setMaxTotal(128);
    jedisPoolConfig.setMaxIdle(128);
    jedisPoolConfig.setMinIdle(10);
    jedisPoolConfig.setMaxWaitMillis(3000);
    jedisPoolConfig.setTestOnBorrow(true);
    jedisPoolConfig.setTestOnReturn(true);
    jedisPoolConfig.setTestWhileIdle(true);
    jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
    jedisPoolConfig.setNumTestsPerEvictionRun(10);
    jedisPoolConfig.setMinEvictableIdleTimeMillis(60000);
    jedisPoolConfig.setLifo(true);
    jedisPoolConfig.setBlockWhenExhausted(true);
    jedisPoolConfig.setJmxEnabled(false);
    jedisPoolConfig.setJmxNamePrefix("jedis");
    return jedisPoolConfig;
  }

  @Bean(destroyMethod = "close") // chuyển sang đây
  public JedisPool jedisPool(JedisPoolConfig jedisPoolConfig) {
    return new JedisPool(
        jedisPoolConfig,
        redisHost,
        redisPort,
        timeout,
        redisPassword.isEmpty() ? null : redisPassword,
        database);
  }

  /** Optional: Tạo Jedis bean đơn lẻ nếu cần */
}
