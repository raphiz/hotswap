group = "io.github.raphiz"

val rawVersion = System.getenv("PROJECT_VERSION") ?: "SNAPSHOT"
version = rawVersion.removePrefix("v")
