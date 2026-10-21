package com.hopper.fintech.flights.examples;

import com.hopper.fintech.flights.sdk.HtsFlightsClient;
import com.hopper.fintech.flights.sdk.HtsEnvironment;
import com.hopper.fintech.flights.sdk.payments.SpreedlyTokenizer;

final class Env {
    private Env() {
    }

    static HtsFlightsClient htsClient() {
        HtsFlightsClient.Builder builder = HtsFlightsClient.builder()
            .apiKey(optional("HTS_API_KEY", "unused"))
            .debugging(Boolean.parseBoolean(optional("HTS_DEBUG", "false")));
        String cfarBaseUrl = System.getenv("HTS_CFAR_BASE_URL");
        String dgBaseUrl = System.getenv("HTS_DG_BASE_URL");
        if (cfarBaseUrl != null && !cfarBaseUrl.isEmpty() && dgBaseUrl != null && !dgBaseUrl.isEmpty()) {
            builder.cfarBaseUrl(cfarBaseUrl).dgBaseUrl(dgBaseUrl);
        } else {
            builder.environment(HtsEnvironment.valueOf(optional("HTS_ENV", "STAGING")));
            required("HTS_API_KEY");
        }
        HtsFlightsClient client = builder.build();
        for (String header : optional("HTS_EXTRA_HEADERS", "").split(",")) {
            int eq = header.indexOf('=');
            if (eq > 0) {
                String name = header.substring(0, eq).trim();
                String value = header.substring(eq + 1).trim();
                client.cfar().apiClient().addDefaultHeader(name, value);
                client.dg().apiClient().addDefaultHeader(name, value);
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
