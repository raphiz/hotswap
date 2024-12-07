plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.palantir.gradle.gitversion:gradle-git-version:3.1.0")
}

tasks.test {
    useJUnitPlatform()
}
