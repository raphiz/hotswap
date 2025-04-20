plugins {
    id("io.github.raphiz.hotswap.java-library")
    id("io.github.raphiz.hotswap.release-artifact")
    id("com.gradle.plugin-publish") version "1.3.1"
    id("com.gradleup.shadow") version "8.3.6"
}

tasks.shadowJar.configure {
    archiveClassifier = ""
}

gradlePlugin {
    website.set("https://github.com/raphiz/hotswap")
    vcsUrl.set("https://github.com/raphiz/hotswap")
    plugins {
        create("hotswap") {
            id = "io.github.raphiz.hotswap"
            displayName = "Hotswap Gradle Plugin"
            description =
                "Java library & Gradle plugin providing lightweight and efficient hot reloading (similar to Spring Boot Devtools) for ANY JVM application"
            implementationClass = "io.github.raphiz.hotswap.gradle.HotswapGradlePlugin"
            tags = listOf("hotswap", "reload", "refresh")
        }
    }
}

tasks.validatePlugins.configure {
    enableStricterValidation = true
}

dependencies {
    implementation(project(":lib"))
    testImplementation(testFixtures(project(":lib")))
}
