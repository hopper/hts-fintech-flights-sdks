package com.hopper.fintech.flights.sdk;

public enum HtsEnvironment {
    STAGING("https://fintech.staging.htsapis.com"),
    PRODUCTION("https://fintech.htsapis.com");

    private final String host;

    HtsEnvironment(String host) {
        this.host = host;
    }

    public String flightsCfarBaseUrl() {
        return host + "/flights-cfar/v2";
    }

    public String flightsDgBaseUrl() {
        return host + "/flights-dafar/v2";
    }
}
