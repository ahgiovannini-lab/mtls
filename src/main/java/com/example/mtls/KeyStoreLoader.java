package com.example.mtls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public final class KeyStoreLoader {
    private KeyStoreLoader() {
    }

    public static KeyStore load(Path path, String type, String password) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = Files.newInputStream(path)) {
            keyStore.load(inputStream, password != null ? password.toCharArray() : null);
        }
        return keyStore;
    }
}
