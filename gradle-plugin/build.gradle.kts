plugins {
    id("io.github.raphiz.hotswap.java-library")
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("hotswap") {
            id = "io.github.raphiz.hotswap"
            implementationClass = "io.github.raphiz.hotswap.gradle.HotswapGradlePlugin"
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
