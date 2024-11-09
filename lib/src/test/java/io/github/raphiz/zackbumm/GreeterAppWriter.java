package io.github.raphiz.zackbumm;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GreeterAppWriter implements AutoCloseable {
    public static final String JAVA_LANGUAGE_VERSION = "17";
    private final Path sourceDirectory;
    private final Path buildDirectory;
    private final Path outputLog;
    private final String packageName;
    private final String className;

    public GreeterAppWriter(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
        try {
            this.sourceDirectory = Files.createTempDirectory("source");
            this.buildDirectory = Files.createTempDirectory("build");
            this.outputLog = Files.createTempFile("output", ".log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getBuildDirectory() {
        return buildDirectory;
    }

    public void writeCodeWithMessage(String messageCode) throws IOException {
        String javaCode = """
                package %s;
                import java.nio.file.*;
                
                public class %s {
                    public static void main(String[] args) throws Exception {
                        while (true) {
                            Files.writeString(Path.of("%s"), "%s");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }
                """.formatted(packageName, className, outputLog.toAbsolutePath(), messageCode);

        Files.createDirectories(getJavaFilePath().getParent());
        Files.writeString(getJavaFilePath(), javaCode);
    }

    public void writeAppWithSlowShutdownProcedure(Duration shutdownDuration) throws IOException {
        String javaCode = """
                package %s;
                import java.nio.file.*;
                
                public class %s {
                    public static void main(String[] args) throws Exception {
                        while (true) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.sleep(%s);
                                break;
                            }
                        }
                    }
                }
                """.formatted(packageName, className, shutdownDuration.toMillis());

        Files.createDirectories(getJavaFilePath().getParent());
        Files.writeString(getJavaFilePath(), javaCode);
    }

    public void compile() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("javac", "--release", JAVA_LANGUAGE_VERSION, "-d",
                buildDirectory.toAbsolutePath().toString(),
                getJavaFilePath().toAbsolutePath().toString()
        )
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Compilation timed out");
        assertEquals(0, process.exitValue(), "Compilation failed with unexpected exit code");
    }

    public void assertLoggedMessage(String message) {
        try {
            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> Files.readString(outputLog).contains(message));
        } catch (Exception e) {
            Assertions.fail("Timeout waiting for log message: '" + message + "'", e);
        }
    }

    @Override
    public void close() throws Exception {
        Files.delete(outputLog);
        deleteRecursively(sourceDirectory);
    }

    private void deleteRecursively(Path path) throws IOException {
        try (var dirStream = Files.walk(path)) {
            dirStream
                    .map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
        }
    }

    private Path getJavaFilePath() {
        String packageDirectories = packageName.replace(".", FileSystems.getDefault().getSeparator());
        String classFileName = className + ".java";
        return sourceDirectory.resolve(packageDirectories).resolve(classFileName);
    }

    URLClassLoader createFakeParentClassLoader() {
        try {
            return new URLClassLoader(new URL[]{getBuildDirectory().toUri().toURL()}, ClassLoader.getSystemClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
