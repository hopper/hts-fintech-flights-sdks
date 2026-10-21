package com.hopper.fintech.flights.sdk;

import com.hopper.fintech.flights.cfar.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtsFlightsClientTest {
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

    private HtsFlightsClient client() {
        return HtsFlightsClient.builder()
            .cfarBaseUrl(server.url("/flights-cfar/v2").toString())
            .dgBaseUrl(server.url("/flights-dafar/v2").toString())
            .apiKey("test-key")
            .build();
    }

    @Test
    void cfarSendsApiKeyAndUserAgentOnEveryRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"message\":\"not found\"}"));

        ApiException error = assertThrows(ApiException.class, () -> client().cfar().contracts().getContractsId("abc").execute());

        assertEquals(404, error.getCode());
        RecordedRequest request = server.takeRequest();
        assertEquals("/flights-cfar/v2/contracts/abc", request.getPath());
        assertEquals("test-key", request.getHeader("X-Api-Key"));
        assertTrue(request.getHeader("User-Agent").startsWith("hts-fintech-flights-java/"));
    }

    @Test
    void dgUsesTheSameKeyAgainstItsOwnBasePath() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"message\":\"not found\"}"));

        com.hopper.fintech.flights.dafar.ApiException error = assertThrows(
            com.hopper.fintech.flights.dafar.ApiException.class,
            () -> client().dg().contracts().getContractsId("abc").execute());

        assertEquals(404, error.getCode());
        RecordedRequest request = server.takeRequest();
        assertEquals("/flights-dafar/v2/contracts/abc", request.getPath());
        assertEquals("test-key", request.getHeader("X-Api-Key"));
        assertTrue(request.getHeader("User-Agent").startsWith("hts-fintech-flights-java/"));
    }

    @Test
    void productsShareOneHttpClient() {
        HtsFlightsClient client = client();
        assertSame(client.cfar().apiClient().getHttpClient(), client.dg().apiClient().getHttpClient());
    }

    @Test
    void environmentSelectsBothBaseUrls() {
        HtsFlightsClient client = HtsFlightsClient.create(HtsEnvironment.STAGING, "k");
        assertEquals("https://fintech.staging.htsapis.com/flights-cfar/v2", client.cfar().apiClient().getBasePath());
        assertEquals("https://fintech.staging.htsapis.com/flights-dafar/v2", client.dg().apiClient().getBasePath());
    }

    @Test
    void timeoutsApplyToBothProducts() {
        HtsFlightsClient client = HtsFlightsClient.builder()
            .environment(HtsEnvironment.STAGING)
            .apiKey("k")
            .readTimeout(java.time.Duration.ofSeconds(7))
            .build();
        assertEquals(7000, client.cfar().apiClient().getReadTimeout());
        assertEquals(7000, client.dg().apiClient().getReadTimeout());
    }
}
