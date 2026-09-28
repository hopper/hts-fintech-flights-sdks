description = "HTS Fintech Flights SDK: generated API clients plus payment tokenization helpers"

dependencies {
    api(project(":flights-cfar-client"))
    api(project(":spreedly-client"))
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
