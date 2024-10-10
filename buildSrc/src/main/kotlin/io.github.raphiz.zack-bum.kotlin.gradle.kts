plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
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
