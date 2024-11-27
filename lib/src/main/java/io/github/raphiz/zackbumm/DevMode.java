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
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DevMode {
    private static final Logger logger = LoggerHelpers.logger();

    public static void main(String[] args) throws IOException {
        // TODO: Pass args to main app (and verify it!)
        String mainClass = System.getProperty("zackbumm.mainClass");
        List<String> packagePrefixes = Arrays.stream(System.getProperty("zackbumm.packagePrefixes").split(",")).toList();
        Set<Path> classesOutputDirectories = Arrays.stream(System.getProperty("zackbumm.classesOutputs").split(File.pathSeparator))
                .map(Path::of)
                .collect(Collectors.toSet());

        startDevMode(
                mainClass,
                packagePrefixes,
                classesOutputDirectories,
                Duration.ofSeconds(5), // TODO: Make configurable
                Duration.ofMillis(10) // TODO: Make configurable
        );
    }

    public static void startDevMode(
            String mainClass,
            Collection<String> packagePrefixes,
            Set<Path> classesOutputDirectories,
            Duration shutdownPollingInterval,
            Duration debounceDuration
    ) throws IOException {
        // TODO: Later -> Add support for jars (gradle modules)
        URL[] classPathUrls = classesOutputDirectories.stream()
                .map(DevMode::toUrl)
                .toArray(URL[]::new);

        List<PathMatcher> restartMatchers = classesOutputDirectories.stream()
                .map(Path::toAbsolutePath)
                .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it + "/**"))
                .toList();

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

        new FileSystemWatcher(classesOutputDirectories, fileSystemEvent -> {
            if (restartMatchers.stream().anyMatch(matcher -> matcher.matches(fileSystemEvent.path()))) {
                // Skip delete events for class files during recompilation
                if (fileSystemEvent.eventType() != EventType.DELETED) {
                    restartDebouncer.submit(fileSystemEvent.path(), fileSystemEvent.eventType());
                }
            }
        }).start();
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
