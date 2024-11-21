plugins {
    id("io.github.raphiz.zackbumm.java-library")
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("my-binary-plugin") {
            id = "io.github.raphiz.zackboom"
            implementationClass = "io.github.raphiz.zackboom.gradle.ZackbummGradlePlugin"
        }
    }
}

dependencies {
    testImplementation(testFixtures(project(":lib")))
}
