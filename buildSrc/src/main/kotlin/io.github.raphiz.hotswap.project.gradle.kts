plugins {
    id("com.palantir.git-version")
}

group = "io.github.raphiz"

version =
    run {
        val gitVersion: groovy.lang.Closure<String> by extra
        val version = gitVersion()
        if (version.startsWith("v")) {
            version.substring(1)
        } else {
            version
        }
    }
