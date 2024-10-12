plugins {
    id("io.github.raphiz.zack-bum.kotlin")
    `java-library`
}

// see https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions { unix("755") }
    filePermissions { unix("644") }
}

tasks.withType<Test>().configureEach {
    val loggingPropertiesFile =
        project.rootProject.layout.projectDirectory
            .file("buildSrc/src/main/resources/logging.properties")
            .asFile
    if (!loggingPropertiesFile.exists()) {
        throw GradleException("Cannot find logging.properties in ${loggingPropertiesFile.absolutePath}")
    }
    systemProperty("java.util.logging.config.file", loggingPropertiesFile.absolutePath)
}
