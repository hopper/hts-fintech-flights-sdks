package com.hopper.hts.fintech.flights.examples;

import com.hopper.hts.fintech.flights.sdk.FlightsCfarClient;
import com.hopper.hts.fintech.flights.sdk.HtsEnvironment;
import com.hopper.hts.fintech.flights.sdk.payments.SpreedlyTokenizer;

final class Env {
    private Env() {
    }

    static FlightsCfarClient htsClient() {
        FlightsCfarClient.Builder builder = FlightsCfarClient.builder()
            .apiKey(optional("HTS_API_KEY", "unused"))
            .debugging(Boolean.parseBoolean(optional("HTS_DEBUG", "false")));
        String baseUrl = System.getenv("HTS_BASE_URL");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        } else {
            builder.environment(HtsEnvironment.valueOf(optional("HTS_ENV", "STAGING")));
            required("HTS_API_KEY");
        }
        FlightsCfarClient client = builder.build();
        for (String header : optional("HTS_EXTRA_HEADERS", "").split(",")) {
            int eq = header.indexOf('=');
            if (eq > 0) {
                client.apiClient().addDefaultHeader(header.substring(0, eq).trim(), header.substring(eq + 1).trim());
            }
        }
        return client;
    }

    static SpreedlyTokenizer spreedlyTokenizerOrNull() {
        String environmentKey = System.getenv("SPREEDLY_ENVIRONMENT_KEY");
        if (environmentKey == null) {
            return null;
        }
        SpreedlyTokenizer.Builder builder = SpreedlyTokenizer.builder()
            .environmentKey(environmentKey)
            .accessSecret(required("SPREEDLY_ACCESS_SECRET"));
        String certificateToken = System.getenv("SPREEDLY_CERTIFICATE_TOKEN");
        if (certificateToken != null) {
            builder.encryption(certificateToken, required("SPREEDLY_PUBLIC_KEY"));
        }
        return builder.build();
    }

    static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Environment variable " + name + " is required");
        }
        return value;
    }

    static String optional(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
