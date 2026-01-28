package com.example.mtls;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

@Validated
@ConfigurationProperties(prefix = "partner.mtls")
public class PartnerMtlsProperties {
    private static final Logger logger = LoggerFactory.getLogger(PartnerMtlsProperties.class);

    private boolean enabled = true;

    @NotBlank
    private String clientKeystorePath;

    @NotBlank
    private String clientKeystorePassword;

    @NotBlank
    private String clientKeystoreType = "PKCS12";

    @NotNull
    private ServerValidation serverValidation = new ServerValidation();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientKeystorePath() {
        return clientKeystorePath;
    }

    public void setClientKeystorePath(String clientKeystorePath) {
        this.clientKeystorePath = clientKeystorePath;
    }

    public String getClientKeystorePassword() {
        return clientKeystorePassword;
    }

    public void setClientKeystorePassword(String clientKeystorePassword) {
        this.clientKeystorePassword = clientKeystorePassword;
    }

    public String getClientKeystoreType() {
        return clientKeystoreType;
    }

    public void setClientKeystoreType(String clientKeystoreType) {
        this.clientKeystoreType = clientKeystoreType;
    }

    public ServerValidation getServerValidation() {
        return serverValidation;
    }

    public void setServerValidation(ServerValidation serverValidation) {
        this.serverValidation = serverValidation;
    }

    @PostConstruct
    public void validateAndLog() {
        if (!enabled) {
            logger.info("mTLS outbound is disabled by configuration");
            return;
        }

        String mode = serverValidation.getMode().name();
        if (serverValidation.getMode() == ServerValidationMode.TRUSTSTORE) {
            require(serverValidation.getTruststorePath(), "truststore-path is required when mode=TRUSTSTORE");
            require(serverValidation.getTruststorePassword(), "truststore-password is required when mode=TRUSTSTORE");
        } else if (serverValidation.getMode() == ServerValidationMode.PIN_FINGERPRINT) {
            require(serverValidation.getPinnedCertSha256Hex(), "pinned-cert-sha256-hex is required when mode=PIN_FINGERPRINT");
        } else if (serverValidation.getMode() == ServerValidationMode.PIN_SPKI) {
            require(serverValidation.getPinnedSpkiSha256Base64(), "pinned-spki-sha256-base64 is required when mode=PIN_SPKI");
        }

        logger.info("mTLS outbound enabled. Server validation mode: {}", mode);
        logger.info("Client keystore path: {}, type: {}", clientKeystorePath, clientKeystoreType);
        if (serverValidation.getMode() == ServerValidationMode.TRUSTSTORE) {
            logger.info("Truststore path: {}, type: {}", serverValidation.getTruststorePath(), serverValidation.getTruststoreType());
        }
    }

    private static void require(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }

    public enum ServerValidationMode {
        TRUSTSTORE,
        PIN_FINGERPRINT,
        PIN_SPKI;

        public static ServerValidationMode fromString(String value) {
            return ServerValidationMode.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public static class ServerValidation {
        @NotNull
        private ServerValidationMode mode = ServerValidationMode.TRUSTSTORE;

        private String truststorePath;

        private String truststorePassword;

        private String truststoreType = "PKCS12";

        private String pinnedCertSha256Hex;

        private String pinnedSpkiSha256Base64;

        public ServerValidationMode getMode() {
            return mode;
        }

        public void setMode(ServerValidationMode mode) {
            this.mode = mode;
        }

        public String getTruststorePath() {
            return truststorePath;
        }

        public void setTruststorePath(String truststorePath) {
            this.truststorePath = truststorePath;
        }

        public String getTruststorePassword() {
            return truststorePassword;
        }

        public void setTruststorePassword(String truststorePassword) {
            this.truststorePassword = truststorePassword;
        }

        public String getTruststoreType() {
            return truststoreType;
        }

        public void setTruststoreType(String truststoreType) {
            this.truststoreType = truststoreType;
        }

        public String getPinnedCertSha256Hex() {
            return pinnedCertSha256Hex;
        }

        public void setPinnedCertSha256Hex(String pinnedCertSha256Hex) {
            this.pinnedCertSha256Hex = pinnedCertSha256Hex;
        }

        public String getPinnedSpkiSha256Base64() {
            return pinnedSpkiSha256Base64;
        }

        public void setPinnedSpkiSha256Base64(String pinnedSpkiSha256Base64) {
            this.pinnedSpkiSha256Base64 = pinnedSpkiSha256Base64;
        }
    }
}
