import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            afterEvaluate {
                // Do not publish test fixtures
                val component = components["java"] as AdhocComponentWithVariants
                listOf("testFixturesApiElements", "testFixturesRuntimeElements").forEach {
                    component.withVariantsFromConfiguration(configurations[it]) { skip() }
                }
                from(component)
            }
            pom {
                name.set("${project.group}:${project.name}")
                description.set(project.description)
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

signing {
    if (project.hasProperty("signingKey")) {
        val signingKey: String by project
        val signingPassword: String by project
        useInMemoryPgpKeys(signingKey.base64Decode(), signingPassword)
    } else {
        useGpgCmd()
    }
    sign(publishing.publications)
}

fun String.base64Decode() = String(Base64.getDecoder().decode(this)).trim()
