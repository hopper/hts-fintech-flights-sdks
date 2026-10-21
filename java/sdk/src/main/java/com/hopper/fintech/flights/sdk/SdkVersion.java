package com.hopper.fintech.flights.sdk;

final class SdkVersion {
    static final String USER_AGENT = "hts-fintech-flights-java/" + resolve();

    private SdkVersion() {
    }

    private static String resolve() {
        String version = SdkVersion.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }
}
