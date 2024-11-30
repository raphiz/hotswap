plugins {
    id("io.github.raphiz.zackbumm.java-library")
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("my-binary-plugin") {
            id = "io.github.raphiz.zackbumm"
            implementationClass = "io.github.raphiz.zackbumm.gradle.ZackbummGradlePlugin"
        }
    }
}

dependencies {
    implementation(project(":lib"))
    testImplementation(testFixtures(project(":lib")))
}
