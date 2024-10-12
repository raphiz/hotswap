plugins {
    id("io.github.raphiz.zack-bum.java-library")
}

dependencies {
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("io.strikt:strikt-core:0.34.0")
    testImplementation("org.slf4j:jul-to-slf4j:2.0.16")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}
