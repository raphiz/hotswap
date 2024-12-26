plugins {
    id("io.github.raphiz.hotswap.java-library")
    id("io.github.raphiz.hotswap.release-artifact")
    `java-test-fixtures`
}

description = "Library that provides hot swap capabilities"

dependencies {
    testImplementation("org.awaitility:awaitility:4.2.0")
    testFixturesImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.slf4j:jul-to-slf4j:2.0.16")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}
