package com.example.mtls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;

class PinningUtilsTest {
    @Test
    void normalizeFingerprintAcceptsColonsAndSpaces() {
        String input = "90:C0:EF:BC:96:79 47:DF";
        assertThat(PinningUtils.normalizeHexFingerprint(input)).isEqualTo("90c0efbc967947df");
    }

    @Test
    void calculatesFingerprintAndSpkiPins() throws Exception {
        X509Certificate certificate = loadCertificate();
        String fingerprint = PinningUtils.certificateFingerprintSha256Hex(certificate);
        String spki = PinningUtils.certificateSpkiSha256Base64(certificate);

        assertThat(fingerprint)
            .isEqualTo("90c0efbc967947df58cd95c93dd2dac9325a2c9163b65ea087be7a16236f9279");
        assertThat(spki)
            .isEqualTo("xkvcWc4qi4imQvQ2w559daHgN0x/baVpKnlPanbTbqM=");
    }

    private X509Certificate loadCertificate() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/certs/test-cert.pem")) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(inputStream);
        }
    }
}
