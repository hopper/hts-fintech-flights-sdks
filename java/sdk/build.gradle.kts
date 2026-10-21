description = "HTS Fintech Flights SDK: one client for the CFAR and Disruption Guarantee APIs, plus payment tokenization helpers"

dependencies {
    api(project(":flights-cfar-client"))
    api(project(":flights-dafar-client"))
    api(project(":spreedly-client"))
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "hts-fintech-flights-java",
            "Implementation-Version" to project.version,
        )
    }
}
