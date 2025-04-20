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
