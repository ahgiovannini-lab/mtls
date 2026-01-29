package com.example.mtls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "partner.mtls", name = { "enabled", "smoke.enabled" }, havingValue = "true")
public class PartnerMtlsSmokeRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(PartnerMtlsSmokeRunner.class);

    private final RestTemplate partnerRestTemplate;
    private final String url;

    public PartnerMtlsSmokeRunner(RestTemplate partnerRestTemplate,
                                  @Value("${partner.mtls.smoke.url}") String url) {
        this.partnerRestTemplate = partnerRestTemplate;
        this.url = url;
    }

    @Override
    public void run(String... args) {
        logger.info("Running mTLS smoke check against {}", url);
        try {
            var response = partnerRestTemplate.getForEntity(url, String.class);
            logger.info("mTLS smoke check OK: status {}", response.getStatusCode());
        } catch (Exception ex) {
            logger.error("mTLS smoke check failed", ex);
        }
    }
}
