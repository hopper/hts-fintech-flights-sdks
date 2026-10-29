package com.hopper.fintech.flights.sdk;

import com.hopper.fintech.flights.cfar.ApiClient;
import com.hopper.fintech.flights.cfar.api.AnalyticsApi;
import com.hopper.fintech.flights.cfar.api.ContractManagementApi;
import com.hopper.fintech.flights.cfar.api.HtsSelfServiceExerciseApi;
import com.hopper.fintech.flights.cfar.api.OfferMerchandisingApi;
import com.hopper.fintech.flights.cfar.api.PartnerManualExerciseApi;
import com.hopper.fintech.flights.cfar.api.PartnerSelfServiceExerciseApi;
import okhttp3.OkHttpClient;

public final class FlightsCfar {
    private final ApiClient apiClient;

    FlightsCfar(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    static ApiClient apiClient(OkHttpClient httpClient, String baseUrl, String apiKey) {
        ApiClient apiClient = new ApiClient(httpClient);
        apiClient.setBasePath(baseUrl);
        apiClient.setApiKey(apiKey);
        apiClient.setUserAgent(SdkVersion.USER_AGENT);
        return apiClient;
    }

    public ApiClient apiClient() {
        return apiClient;
    }

    public OfferMerchandisingApi offers() {
        return new OfferMerchandisingApi(apiClient);
    }

    public ContractManagementApi contracts() {
        return new ContractManagementApi(apiClient);
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

    public AnalyticsApi analytics() {
        return new AnalyticsApi(apiClient);
    }
}
