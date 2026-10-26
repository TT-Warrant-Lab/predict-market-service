package com.fadx.predict_market_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PredictMarketServiceApplicationTests {
  private final MockMvc mockMvc;

  @Autowired
  PredictMarketServiceApplicationTests(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void contextLoads() {}

  @Test
  void rejectsRequestsWithoutInternalKey() throws Exception {
    mockMvc.perform(get("/api/not-found")).andExpect(status().isUnauthorized());
  }

  @Test
  void acceptsConfiguredInternalKeyBeforeRouting() throws Exception {
    mockMvc
        .perform(get("/api/not-found").header("X-Internal-Key", "test-internal-key"))
        .andExpect(status().isNotFound());
  }
}
