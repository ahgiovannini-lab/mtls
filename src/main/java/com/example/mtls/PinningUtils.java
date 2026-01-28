package com.example.mtls;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public final class PinningUtils {
    private PinningUtils() {
    }

    public static String normalizeHexFingerprint(String input) {
        if (input == null) {
            return null;
        }
        return input.replace(":", "")
            .replace(" ", "")
            .replace("\t", "")
            .replace("\n", "")
            .replace("\r", "")
            .toLowerCase();
    }

    public static String sha256Hex(byte[] data) {
        byte[] digest = sha256(data);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static String sha256Base64(byte[] data) {
        return Base64.getEncoder().encodeToString(sha256(data));
    }

    public static String certificateFingerprintSha256Hex(X509Certificate certificate) throws CertificateEncodingException {
        return sha256Hex(certificate.getEncoded());
    }

    public static String certificateSpkiSha256Base64(X509Certificate certificate) {
        return sha256Base64(certificate.getPublicKey().getEncoded());
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
