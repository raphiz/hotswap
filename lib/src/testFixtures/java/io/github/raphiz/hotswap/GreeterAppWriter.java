package io.github.raphiz.hotswap;

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
    public static final String PACKAGE_PREFIX = "com.example";
    public  static final String CLASS_NAME = "HelloWorldApp";

    private final Path sourceDirectory;
    private final Path projectDirectory;
    private final Path buildDirectory;
    private final Path outputLog;
    private final String packageName;
    private final String className;

    public GreeterAppWriter() {
        this(PACKAGE_PREFIX, CLASS_NAME);
    }

    public GreeterAppWriter(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
        try {
            this.projectDirectory = Files.createTempDirectory("source");
            this.sourceDirectory = projectDirectory.resolve("src/main/java");
            this.buildDirectory = Files.createTempDirectory("build");
            this.outputLog = Files.createTempFile("output", ".log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getProjectDirectory() {
        return projectDirectory;
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
                        System.out.println("Greeter app started");
                        if(!isClassAvailable("io.github.raphiz.hotswap.DevMode")){
                            System.out.println("IMPORTANT: DevMode class is not on the classpath!");
                        }
                        String argsAsString = String.join(", ", args);
                        while (true) {
                            Files.writeString(Path.of("%s"), String.join(" ", "%s", argsAsString));
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                    public static boolean isClassAvailable(String className) {
                        try {
                            Class.forName(className);
                            return true; // Class is available
                        } catch (ClassNotFoundException e) {
                            return false; // Class is not in the classpath
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

    public void assertOutputsMessage(String message) {
        assertOutputsMessage(message, Duration.ofSeconds(5));
    }

    public void assertOutputsMessage(String message, Duration timeout) {
        try {
            Awaitility.await()
                    .atMost(timeout)
                    .pollInterval(Duration.ofMillis(10))
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
