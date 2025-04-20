plugins {
    id("io.github.raphiz.hotswap.java-library")
    id("io.github.raphiz.hotswap.release-artifact")
    `java-test-fixtures`
}

description = "Library that provides hot swap capabilities"

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "hotswap-${project.name}"
            afterEvaluate {
                // Do not publish test fixtures
                val component = components["java"] as AdhocComponentWithVariants
                listOf("testFixturesApiElements", "testFixturesRuntimeElements").forEach {
                    component.withVariantsFromConfiguration(configurations[it]) { skip() }
                }
                from(component)

                pom.description = project.description
            }
            pom {
                name.set(artifactId)
                url.set("https://www.github.com/raphiz/hotswap")

                scm {
                    connection.set("scm:git:git://github.com/raphiz/hotswap.git")
                    developerConnection.set("scm:git:ssh://github.com:raphiz/hotswap.git")
                    url.set("https://www.github.com/raphiz/hotswap")
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/mit-license")
                    }
                }

                developers {
                    developer {
                        id.set("raphiz")
                        name.set("Raphael Zimmermann")
                        email.set("oss@raphael.li")
                    }
                }
            }
        }
    }
}

dependencies {
    testImplementation("org.awaitility:awaitility:4.2.0")
    testFixturesImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.slf4j:jul-to-slf4j:2.0.16")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}
