description = "Runnable examples against the HTS staging environment"

dependencies {
    implementation(project(":sdk"))
}

val examples = mapOf(
    "runCreateContract" to "com.hopper.hts.fintech.flights.examples.CreateOfferAndContract",
    "runExerciseContract" to "com.hopper.hts.fintech.flights.examples.ExerciseContract",
    "runTokenizeCard" to "com.hopper.hts.fintech.flights.examples.TokenizeCard",
)

examples.forEach { (taskName, mainClassName) ->
    tasks.register<JavaExec>(taskName) {
        group = "examples"
        description = "Runs $mainClassName"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(mainClassName)
        args = (project.findProperty("exampleArgs") as String?)?.split(" ") ?: emptyList()
    }
}
