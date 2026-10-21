import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish") version "0.33.0" apply false
}

val repositoryUrl = "https://github.com/hopper/hts-fintech-flights-sdks"

val publishedArtifacts = mapOf(
    "flights-cfar-client" to "hts-fintech-flights-cfar-client",
    "flights-dafar-client" to "hts-fintech-flights-dafar-client",
    "spreedly-client" to "hts-fintech-spreedly-client",
    "sdk" to "hts-fintech-flights-sdk",
)

allprojects {
    group = "com.hopper.cloud"
    version = providers.gradleProperty("sdkVersion").get()

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
        isFailOnError = false
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

configure(subprojects.filter { it.name.endsWith("-client") }) {
    dependencies {
        "api"("com.squareup.okhttp3:okhttp:4.12.0")
        "api"("com.google.code.gson:gson:2.9.1")
        "implementation"("com.squareup.okhttp3:logging-interceptor:4.12.0")
        "implementation"("io.gsonfire:gson-fire:1.9.0")
        "implementation"("io.swagger:swagger-annotations:1.6.8")
        "implementation"("com.google.code.findbugs:jsr305:3.0.2")
        "implementation"("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6")
        "implementation"("jakarta.annotation:jakarta.annotation-api:1.3.5")
        "implementation"("org.apache.commons:commons-lang3:3.17.0")
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.3")
    }
}

configure(subprojects.filter { it.name in publishedArtifacts }) {
    apply(plugin = "com.vanniktech.maven.publish")

    configure<MavenPublishBaseExtension> {
        publishToMavenCentral(
            SonatypeHost.CENTRAL_PORTAL,
            automaticRelease = providers.gradleProperty("mavenCentralAutomaticRelease").getOrElse("true").toBoolean(),
        )
        if (providers.gradleProperty("signingInMemoryKey").isPresent) {
            signAllPublications()
        }
        coordinates(project.group.toString(), publishedArtifacts.getValue(project.name), project.version.toString())

        pom {
            name.set(publishedArtifacts.getValue(project.name))
            description.set(provider { project.description ?: "HTS Fintech Flights SDK" })
            url.set(repositoryUrl)
            inceptionYear.set("2026")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("hopper")
                    name.set("Hopper Technology Solutions")
                    url.set("https://github.com/hopper")
                }
            }
            scm {
                url.set(repositoryUrl)
                connection.set("scm:git:git://github.com/hopper/hts-fintech-flights-sdks.git")
                developerConnection.set("scm:git:ssh://git@github.com/hopper/hts-fintech-flights-sdks.git")
            }
        }
    }
}
