package io.github.raphiz.zackbumm;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class DevMode {
    private static final Logger logger = LoggerHelpers.logger();

    public static void main(String[] args) throws IOException {
        // TODO: Pass args to main app (and verify it!)
        String mainClass = System.getProperty("zackbumm.mainClass");
        List<String> packagePrefixes = Arrays.stream(System.getProperty("zackbumm.packagePrefixes").split(",")).toList();
        String[] classesOutputDirectories = System.getProperty("zackbumm.classesOutputs").split(File.pathSeparator);
        Path workspace = Path.of(System.getProperty("user.dir")).toAbsolutePath();

        // TODO: Later -> Add support for jars (gradle modules)
        URL[] classPathUrls = Arrays.stream(classesOutputDirectories)
                .map(DevMode::toUrl)
                .toArray(URL[]::new);

        List<PathMatcher> restartMatchers = Arrays.stream(classesOutputDirectories)
                .map((it) -> Path.of(it).toAbsolutePath())
                .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it + "/**"))
                .toList();

        startDevMode(
                mainClass,
                packagePrefixes,
                classPathUrls,
                restartMatchers,
                workspace,
                Duration.ofSeconds(5), // TODO: Make configurable
                Duration.ofMillis(10) // TODO: Make configurable
        );
    }

    public static void startDevMode(
            String mainClass,
            Collection<String> packagePrefixes,
            URL[] classPathUrls,
            List<PathMatcher> restartMatchers,
            Path workspace,
            Duration shutdownPollingInterval,
            Duration debounceDuration
    ) throws IOException {
        ApplicationLoader applicationLoader = new ApplicationLoader(
                mainClass,
                packagePrefixes,
                classPathUrls,
                shutdownPollingInterval
        );
        applicationLoader.start();

        PathUpdateDebouncer restartDebouncer = new PathUpdateDebouncer(debounceDuration, pathUpdates -> {
            logger.fine(() -> "Restarting due to " + pathUpdates);
            applicationLoader.restart();
        });

        new FileSystemWatcher(workspace, fileSystemEvent -> {
            if (restartMatchers.stream().anyMatch(matcher -> matcher.matches(fileSystemEvent.path()))) {
                // Skip delete events for class files during recompilation
                if (fileSystemEvent.eventType() != EventType.DELETED) {
                    restartDebouncer.submit(fileSystemEvent.path(), fileSystemEvent.eventType());
                }
            }
        }).start();
    }

    private static URL toUrl(String it) {
        try {
            return Path.of(it).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
