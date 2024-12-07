plugins {
    `java-library`
    id("com.palantir.git-version")
}

group = "io.github.raphiz.hotswap"
val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.10.2")
        }
    }
}

project.afterEvaluate {
    val testFixturesConfig = configurations.findByName("testFixturesImplementation")
    if (testFixturesConfig != null) {
        project.dependencies.apply {
            add(testFixturesConfig.name, platform("org.junit:junit-bom:5.10.2"))
            add(testFixturesConfig.name, "org.junit.jupiter:junit-jupiter-engine")
            add(testFixturesConfig.name, "org.junit.jupiter:junit-jupiter-api")
        }
    }
}

// Make sure dependency resolution is reproducible
// See https://docs.gradle.org/current/userguide/resolution_strategy_tuning.html#reproducible-resolution
configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
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
