package com.fadx.predict_market_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

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

  @Test
  void exposesInternalKeyAuthorizationInOpenApi() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.internalKey.type").value("apiKey"))
        .andExpect(jsonPath("$.components.securitySchemes.internalKey.in").value("header"))
        .andExpect(
            jsonPath("$.components.securitySchemes.internalKey.name").value("X-Internal-Key"))
        .andExpect(jsonPath("$.security[0].internalKey").isArray());
  }
}
