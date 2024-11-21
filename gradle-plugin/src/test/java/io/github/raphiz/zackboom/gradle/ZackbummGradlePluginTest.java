package io.github.raphiz.zackboom.gradle;

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
    @ValueSource(strings = {"7.0", "8.11.1"})
    void testRunTaskUntilMessageIsLogged(String gradleVersion) throws IOException {
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
        greeterAppWriter.assertLoggedMessage("Hello World", Duration.ofMinutes(2));

        // TODO: Test drive once the plugin is ready
        // greeterAppWriter.writeCodeWithMessage("Hello Universe");
        // greeterAppWriter.assertLoggedMessage("Hello Universe");
    }

    private void writeGradleFile() throws IOException {
        Path buildFile = greeterAppWriter.getProjectDirectory().resolve("build.gradle.kts");
        Files.writeString(buildFile, """
                plugins {
                    application
                    id("io.github.raphiz.zackboom")
                }
                application {
                    mainClass.set("%s.%s")
                }
                """.stripIndent().formatted(PACKAGE_PREFIX, CLASS_NAME)
        );
    }

}