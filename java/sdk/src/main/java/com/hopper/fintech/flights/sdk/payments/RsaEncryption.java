package com.hopper.fintech.flights.sdk.payments;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class RsaEncryption {
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final PublicKey publicKey;

    RsaEncryption(String base64PublicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64-encoded RSA public key", e);
        }
    }

    String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA encryption failed", e);
        }
    }
}
