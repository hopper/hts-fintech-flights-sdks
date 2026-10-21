package com.hopper.fintech.flights.sdk;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import java.time.Duration;
import java.util.Objects;

public final class HtsFlightsClient {
    private final FlightsCfar cfar;
    private final FlightsDg dg;

    private HtsFlightsClient(FlightsCfar cfar, FlightsDg dg) {
        this.cfar = cfar;
        this.dg = dg;
    }

    public static HtsFlightsClient create(HtsEnvironment environment, String apiKey) {
        return builder().environment(environment).apiKey(apiKey).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public FlightsCfar cfar() {
        return cfar;
    }

    public FlightsDg dg() {
        return dg;
    }

    public static final class Builder {
        private HtsEnvironment environment;
        private String cfarBaseUrl;
        private String dgBaseUrl;
        private String apiKey;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(60);
        private Duration writeTimeout = Duration.ofSeconds(60);
        private boolean debugging;
        private OkHttpClient httpClient;

        private Builder() {
        }

        public Builder environment(HtsEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder cfarBaseUrl(String cfarBaseUrl) {
            this.cfarBaseUrl = cfarBaseUrl;
            return this;
        }

        public Builder dgBaseUrl(String dgBaseUrl) {
            this.dgBaseUrl = dgBaseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder writeTimeout(Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder debugging(boolean debugging) {
            this.debugging = debugging;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public HtsFlightsClient build() {
            Objects.requireNonNull(apiKey, "apiKey is required");
            String cfarUrl = cfarBaseUrl != null ? cfarBaseUrl : environmentUrl(HtsEnvironment::flightsCfarBaseUrl);
            String dgUrl = dgBaseUrl != null ? dgBaseUrl : environmentUrl(HtsEnvironment::flightsDgBaseUrl);
            OkHttpClient shared = sharedHttpClient();
            return new HtsFlightsClient(
                new FlightsCfar(FlightsCfar.apiClient(shared, cfarUrl, apiKey)),
                new FlightsDg(FlightsDg.apiClient(shared, dgUrl, apiKey)));
        }

        private String environmentUrl(java.util.function.Function<HtsEnvironment, String> url) {
            Objects.requireNonNull(environment, "environment or both base URLs are required");
            return url.apply(environment);
        }

        private OkHttpClient sharedHttpClient() {
            OkHttpClient.Builder builder = (httpClient != null ? httpClient : new OkHttpClient()).newBuilder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout);
            if (debugging) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logging);
            }
            return builder.build();
        }
    }
}
