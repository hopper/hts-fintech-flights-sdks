package com.hopper.hts.fintech.flights.sdk;

import com.hopper.hts.fintech.flights.cfar.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightsCfarClientTest {
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
    void sendsApiKeyAndUserAgentOnEveryRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"message\":\"not found\"}"));
        FlightsCfarClient client = FlightsCfarClient.builder()
            .baseUrl(server.url("/flights-cfar/v2").toString())
            .apiKey("test-key")
            .build();

        ApiException error = assertThrows(ApiException.class, () -> client.contracts().getContractsId("abc"));

        assertEquals(404, error.getCode());
        RecordedRequest request = server.takeRequest();
        assertEquals("/flights-cfar/v2/contracts/abc", request.getPath());
        assertEquals("test-key", request.getHeader("X-Api-Key"));
        assertTrue(request.getHeader("User-Agent").startsWith("hts-fintech-flights-java/"));
    }

    @Test
    void environmentSelectsBaseUrl() {
        FlightsCfarClient client = FlightsCfarClient.create(HtsEnvironment.STAGING, "k");
        assertEquals("https://fintech.staging.htsapis.com/flights-cfar/v2", client.apiClient().getBasePath());
    }
}
