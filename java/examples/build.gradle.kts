description = "Runnable examples against the HTS staging environment"

dependencies {
    implementation(project(":sdk"))
}

val examples = mapOf(
    "runCreateCfarContract" to "com.hopper.fintech.flights.examples.CreateCfarContract",
    "runExerciseCfarContract" to "com.hopper.fintech.flights.examples.ExerciseCfarContract",
    "runCreateDgContract" to "com.hopper.fintech.flights.examples.CreateDgContract",
    "runExerciseDgContract" to "com.hopper.fintech.flights.examples.ExerciseDgContract",
    "runTokenizeCard" to "com.hopper.fintech.flights.examples.TokenizeCard",
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
