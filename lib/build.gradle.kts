plugins {
    id("io.github.raphiz.zack-bum.kotlin-library")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("io.strikt:strikt-core:0.34.0")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}
