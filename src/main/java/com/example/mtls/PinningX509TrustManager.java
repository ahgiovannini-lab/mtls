package com.example.mtls;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PinningX509TrustManager implements javax.net.ssl.X509TrustManager {
    private static final Logger logger = LoggerFactory.getLogger(PinningX509TrustManager.class);

    private final javax.net.ssl.X509TrustManager delegate;
    private final PartnerMtlsProperties.ServerValidationMode mode;
    private final String expectedPin;

    public PinningX509TrustManager(javax.net.ssl.X509TrustManager delegate,
                                  PartnerMtlsProperties.ServerValidationMode mode,
                                  String expectedPin) {
        this.delegate = delegate;
        this.mode = mode;
        this.expectedPin = expectedPin;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Server certificate chain is empty");
        }
        X509Certificate leaf = chain[0];
        if (mode == PartnerMtlsProperties.ServerValidationMode.PIN_FINGERPRINT) {
            String actual = PinningUtils.normalizeHexFingerprint(PinningUtils.certificateFingerprintSha256Hex(leaf));
            String expectedNormalized = PinningUtils.normalizeHexFingerprint(expectedPin);
            if (!actual.equals(expectedNormalized)) {
                logger.warn("Server certificate pin mismatch (PIN_FINGERPRINT)");
                throw new CertificateException("Server certificate fingerprint pin mismatch");
            }
        } else if (mode == PartnerMtlsProperties.ServerValidationMode.PIN_SPKI) {
            String actual = PinningUtils.certificateSpkiSha256Base64(leaf);
            if (!actual.equals(expectedPin)) {
                logger.warn("Server certificate pin mismatch (PIN_SPKI)");
                throw new CertificateException("Server certificate SPKI pin mismatch");
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
