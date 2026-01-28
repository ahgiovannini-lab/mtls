package com.example.mtls;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PartnerSslContextFactory {
    private static final Logger logger = LoggerFactory.getLogger(PartnerSslContextFactory.class);

    private final PartnerMtlsProperties properties;

    public PartnerSslContextFactory(PartnerMtlsProperties properties) {
        this.properties = properties;
    }

    public SSLContext build() throws GeneralSecurityException, IOException {
        PartnerMtlsProperties.ServerValidation validation = properties.getServerValidation();
        KeyStore clientKeyStore = KeyStoreLoader.load(Path.of(properties.getClientKeystorePath()),
            properties.getClientKeystoreType(), properties.getClientKeystorePassword());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, properties.getClientKeystorePassword().toCharArray());

        TrustManager[] trustManagers = buildTrustManagers(validation);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());

        logger.info("SSLContext initialized for outbound mTLS using mode {}", validation.getMode());
        return sslContext;
    }

    private TrustManager[] buildTrustManagers(PartnerMtlsProperties.ServerValidation validation)
        throws GeneralSecurityException, IOException {
        if (validation.getMode() == PartnerMtlsProperties.ServerValidationMode.TRUSTSTORE) {
            KeyStore trustStore = KeyStoreLoader.load(Path.of(validation.getTruststorePath()),
                validation.getTruststoreType(), validation.getTruststorePassword());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            logger.debug("Truststore loaded from {}", validation.getTruststorePath());
            return tmf.getTrustManagers();
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        X509TrustManager baseTrustManager = Arrays.stream(tmf.getTrustManagers())
            .filter(X509TrustManager.class::isInstance)
            .map(X509TrustManager.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No X509TrustManager available"));

        if (validation.getMode() == PartnerMtlsProperties.ServerValidationMode.PIN_FINGERPRINT) {
            logger.debug("Using fingerprint pinning validation");
            return new TrustManager[] { new PinningX509TrustManager(baseTrustManager,
                validation.getMode(), validation.getPinnedCertSha256Hex()) };
        }

        logger.debug("Using SPKI pinning validation");
        return new TrustManager[] { new PinningX509TrustManager(baseTrustManager,
            validation.getMode(), validation.getPinnedSpkiSha256Base64()) };
    }
}
