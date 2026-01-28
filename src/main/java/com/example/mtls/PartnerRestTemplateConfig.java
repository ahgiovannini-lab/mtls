package com.example.mtls;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(PartnerMtlsProperties.class)
public class PartnerRestTemplateConfig {
    private static final Logger logger = LoggerFactory.getLogger(PartnerRestTemplateConfig.class);

    private final PartnerSslContextFactory sslContextFactory;

    public PartnerRestTemplateConfig(PartnerSslContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
    }

    @Bean
    @ConditionalOnProperty(prefix = "partner.mtls", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate partnerRestTemplate() throws GeneralSecurityException, IOException {
        ClientHttpRequestFactory requestFactory = httpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setInterceptors(List.of(handshakeLoggingInterceptor()));
        return restTemplate;
    }

    private ClientHttpRequestFactory httpRequestFactory() throws GeneralSecurityException, IOException {
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
            sslContextFactory.build(), new DefaultHostnameVerifier());

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(10))
            .build();

        HttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(TimeValue.ofSeconds(30))
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    private ClientHttpRequestInterceptor handshakeLoggingInterceptor() {
        return (request, body, execution) -> {
            try {
                return execution.execute(request, body);
            } catch (IOException ex) {
                Throwable root = rootCause(ex);
                if (root instanceof SSLHandshakeException) {
                    logger.warn("TLS handshake failed: {}", root.getMessage());
                } else {
                    logger.debug("Request failed: {}", root.getMessage());
                }
                throw ex;
            }
        };
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
