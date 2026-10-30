package com.fadx.predict_market_service.core.config;

import static com.fadx.predict_market_service.core.security.InternalKeyAuthenticationFilter.INTERNAL_KEY_HEADER;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = OpenApiConfig.INTERNAL_KEY_SCHEME))
@SecurityScheme(
    name = OpenApiConfig.INTERNAL_KEY_SCHEME,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = INTERNAL_KEY_HEADER,
    description = "Internal API key")
public class OpenApiConfig {
  static final String INTERNAL_KEY_SCHEME = "internalKey";
}
