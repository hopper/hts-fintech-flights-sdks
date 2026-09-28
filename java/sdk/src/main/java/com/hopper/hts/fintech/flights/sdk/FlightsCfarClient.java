package com.hopper.hts.fintech.flights.sdk;

import com.hopper.hts.fintech.flights.cfar.ApiClient;
import com.hopper.hts.fintech.flights.cfar.api.AnalyticsApi;
import com.hopper.hts.fintech.flights.cfar.api.ContractManagementPostPurchaseApi;
import com.hopper.hts.fintech.flights.cfar.api.HtsSelfServiceExerciseApi;
import com.hopper.hts.fintech.flights.cfar.api.OfferMerchandisingPrePurchaseApi;
import com.hopper.hts.fintech.flights.cfar.api.PartnerEndpointsApi;
import com.hopper.hts.fintech.flights.cfar.api.PartnerManualExerciseApi;
import com.hopper.hts.fintech.flights.cfar.api.PartnerSelfServiceExerciseApi;

import java.time.Duration;
import java.util.Objects;

public final class FlightsCfarClient {
    private final ApiClient apiClient;

    private FlightsCfarClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public static FlightsCfarClient create(HtsEnvironment environment, String apiKey) {
        return builder().environment(environment).apiKey(apiKey).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ApiClient apiClient() {
        return apiClient;
    }

    public OfferMerchandisingPrePurchaseApi offers() {
        return new OfferMerchandisingPrePurchaseApi(apiClient);
    }

    public ContractManagementPostPurchaseApi contracts() {
        return new ContractManagementPostPurchaseApi(apiClient);
    }

    public PartnerSelfServiceExerciseApi partnerSelfServiceExercise() {
        return new PartnerSelfServiceExerciseApi(apiClient);
    }

    public PartnerManualExerciseApi partnerManualExercise() {
        return new PartnerManualExerciseApi(apiClient);
    }

    public HtsSelfServiceExerciseApi htsSelfServiceExercise() {
        return new HtsSelfServiceExerciseApi(apiClient);
    }

    public PartnerEndpointsApi partnerEndpoints() {
        return new PartnerEndpointsApi(apiClient);
    }

    public AnalyticsApi analytics() {
        return new AnalyticsApi(apiClient);
    }

    public static final class Builder {
        private String baseUrl;
        private String apiKey;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(60);
        private Duration writeTimeout = Duration.ofSeconds(60);
        private boolean debugging;

        private Builder() {
        }

        public Builder environment(HtsEnvironment environment) {
            this.baseUrl = environment.flightsCfarBaseUrl();
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

        public FlightsCfarClient build() {
            Objects.requireNonNull(baseUrl, "baseUrl or environment is required");
            Objects.requireNonNull(apiKey, "apiKey is required");
            ApiClient apiClient = new ApiClient();
            apiClient.setBasePath(baseUrl);
            apiClient.setApiKey(apiKey);
            apiClient.setUserAgent(SdkVersion.USER_AGENT);
            apiClient.setConnectTimeout((int) connectTimeout.toMillis());
            apiClient.setReadTimeout((int) readTimeout.toMillis());
            apiClient.setWriteTimeout((int) writeTimeout.toMillis());
            apiClient.setDebugging(debugging);
            return new FlightsCfarClient(apiClient);
        }
    }
}
