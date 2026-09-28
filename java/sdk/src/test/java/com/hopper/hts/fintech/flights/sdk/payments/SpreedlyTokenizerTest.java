package com.hopper.hts.fintech.flights.sdk.payments;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpreedlyTokenizerTest {
    private static final String TOKEN_RESPONSE =
        "{\"transaction\":{\"token\":\"txn_1\",\"payment_method\":{\"token\":\"pm_123\"}}}";

    private MockWebServer server;

    @BeforeEach
    void start() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void tokenizesWithBasicAuthAndReturnsPaymentMethodToken() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201).setBody(TOKEN_RESPONSE));
        SpreedlyTokenizer tokenizer = SpreedlyTokenizer.builder()
            .baseUrl(server.url("/v1").toString())
            .environmentKey("env")
            .accessSecret("secret")
            .build();

        String token = tokenizer.tokenize(card());

        assertEquals("pm_123", token);
        RecordedRequest request = server.takeRequest();
        assertEquals("/v1/payment_methods", request.getPath());
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("env:secret".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedAuth, request.getHeader("Authorization"));
        JsonObject paymentMethod = paymentMethod(request);
        JsonObject creditCard = paymentMethod.getAsJsonObject("credit_card");
        assertEquals("4111111111111111", creditCard.get("number").getAsString());
        assertEquals("123", creditCard.get("verification_value").getAsString());
        assertFalse(paymentMethod.has("encrypted_fields"));
    }

    @Test
    void encryptsCardFieldsWhenEncryptionIsConfigured() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        server.enqueue(new MockResponse().setResponseCode(201).setBody(TOKEN_RESPONSE));
        SpreedlyTokenizer tokenizer = SpreedlyTokenizer.builder()
            .baseUrl(server.url("/v1").toString())
            .environmentKey("env")
            .accessSecret("secret")
            .encryption("cert_abc", publicKey)
            .build();

        tokenizer.tokenize(card());

        JsonObject paymentMethod = paymentMethod(server.takeRequest());
        assertEquals("cert_abc", paymentMethod.get("encryption_certificate_token").getAsString());
        assertEquals("number,month,year,first_name,last_name", paymentMethod.get("encrypted_fields").getAsString());
        JsonObject creditCard = paymentMethod.getAsJsonObject("credit_card");
        assertEquals("4111111111111111", decrypt(keyPair, creditCard.get("number").getAsString()));
        assertEquals("Ada", decrypt(keyPair, creditCard.get("first_name").getAsString()));
        assertEquals("2030", decrypt(keyPair, creditCard.get("year").getAsString()));
        assertEquals("123", creditCard.get("verification_value").getAsString());
        assertTrue(creditCard.get("number").getAsString().length() > 100);
    }

    private static CardDetails card() {
        return CardDetails.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .number("4111111111111111")
            .expirationMonth("12")
            .expirationYear("2030")
            .verificationValue("123")
            .build();
    }

    private static JsonObject paymentMethod(RecordedRequest request) {
        return JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject().getAsJsonObject("payment_method");
    }

    private static String decrypt(KeyPair keyPair, String base64) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        return new String(cipher.doFinal(Base64.getDecoder().decode(base64)), StandardCharsets.UTF_8);
    }
}
