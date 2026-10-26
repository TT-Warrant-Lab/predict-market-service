package com.fadx.predict_market_service.core.security;

import com.fadx.predict_market_service.core.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates every business endpoint with the shared market-service internal key. */
@Component
public class InternalKeyAuthenticationFilter extends OncePerRequestFilter {

  public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

  private final SecurityProperties properties;

  public InternalKeyAuthenticationFilter(SecurityProperties properties) {
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return HttpMethod.OPTIONS.matches(request.getMethod())
        || "/actuator/health".equals(path)
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String configuredKey = properties.getInternalKey();
    String presentedKey = request.getHeader(INTERNAL_KEY_HEADER);
    if (!hasText(configuredKey)
        || !hasText(presentedKey)
        || !MessageDigest.isEqual(
            configuredKey.getBytes(StandardCharsets.UTF_8),
            presentedKey.getBytes(StandardCharsets.UTF_8))) {
      SecurityContextHolder.clearContext();
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid internal key");
      return;
    }

    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "predict-service", null, List.of()));
    chain.doFilter(request, response);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
