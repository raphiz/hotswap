package io.github.raphiz.zackbumm.gradle;

import io.github.raphiz.zackbumm.GreeterAppWriter;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.raphiz.zackbumm.GreeterAppWriter.CLASS_NAME;
import static io.github.raphiz.zackbumm.GreeterAppWriter.PACKAGE_PREFIX;

class ZackbummGradlePluginTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final GreeterAppWriter greeterAppWriter = new GreeterAppWriter();

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.1", "8.11.1"})
    void testRunTaskUntilMessageIsLogged(String gradleVersion) throws IOException, InterruptedException {
        writeGradleFile();
        greeterAppWriter.writeCodeWithMessage("Hello World");

        // Run gradle runner in the background
        executorService.submit(() ->
                GradleRunner.create()
                        .withProjectDir(greeterAppWriter.getProjectDirectory().toFile())
                        .withArguments("run")
                        .withGradleVersion(gradleVersion)
                        .withPluginClasspath()
                        .forwardOutput()
                        .build()
        );

        // Wait for the output to appear.
        // The timeout must be rather large because the entire gradle distribution must be downloaded
        // and extracted on the first run
        greeterAppWriter.assertOutputsMessage("Hello World", Duration.ofMinutes(2));

        // Update greeter message and recompile
        greeterAppWriter.writeCodeWithMessage("Hello Universe");
        GradleRunner.create()
                .withProjectDir(greeterAppWriter.getProjectDirectory().toFile())
                .withArguments("classes")
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
                .forwardOutput()
                .build();

        // Wait for the output to appear.
        greeterAppWriter.assertOutputsMessage("Hello Universe");
    }

    private void writeGradleFile() throws IOException {
        Path buildFile = greeterAppWriter.getProjectDirectory().resolve("build.gradle.kts");
        Files.writeString(buildFile, """
                plugins {
                    application
                    id("io.github.raphiz.zackbumm")
                }
                application {
                    mainClass.set("%s.%s")
                }
                repositories {
                    mavenCentral()
                }
                zackbumm {
                    packagePrefixes.set(listOf("com.example"));
                }
                
                // Dependencies are not actually needed, but it makes the
                // classpath more realistic.
                dependencies {
                    implementation("org.slf4j:jul-to-slf4j:2.0.16")
                    implementation("ch.qos.logback:logback-classic:1.4.14")
                }
                """.stripIndent().formatted(PACKAGE_PREFIX, CLASS_NAME)
        );
        // Enable configuration cache and isolated projects to capture related issues
        Path propertiesFile = greeterAppWriter.getProjectDirectory().resolve("gradle.properties");
        Files.writeString(propertiesFile, """
            org.gradle.configuration-cache=true
            org.gradle.unsafe.isolated-projects=true
        """.stripIndent());
    }

}