package com.fadx.predict_market_service.core.config;

import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MasApiProperties.class)
public class MasRestClientConfig {

  @Bean
  @Primary
  RestClient restClient(RestClient.Builder builder, MasApiProperties properties) {
    return builder.clone().baseUrl(properties.getBaseUrl()).build();
  }

  @Bean("masRestClient")
  RestClient masRestClient(RestClient.Builder builder, MasApiProperties properties) {
    return builder
        .clone()
        .baseUrl(properties.getBaseUrl())
        .requestFactory(insecureRequestFactory())
        .build();
  }

  private static JdkClientHttpRequestFactory insecureRequestFactory() {
    try {
      TrustManager[] trustAllCertificates =
          new TrustManager[] {
            new X509TrustManager() {
              @Override
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }

              @Override
              public void checkClientTrusted(X509Certificate[] chain, String authType) {}

              @Override
              public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            }
          };
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCertificates, new SecureRandom());

      SSLParameters sslParameters = new SSLParameters();
      sslParameters.setEndpointIdentificationAlgorithm("");
      HttpClient httpClient =
          HttpClient.newBuilder().sslContext(sslContext).sslParameters(sslParameters).build();
      return new JdkClientHttpRequestFactory(httpClient);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Cannot configure insecure MAS TLS client", exception);
    }
  }
}
