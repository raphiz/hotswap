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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DevMode {
    private static final Logger logger = LoggerHelpers.logger();

    public static class Configuration {
        final String mainClass;
        final List<String> packagePrefixes;
        final Set<Path> classesOutputDirectories;
        final Duration shutdownPollingInterval;
        final Duration debounceDuration;

        Configuration(String mainClass, List<String> packagePrefixes, Set<Path> classesOutputDirectories, Duration shutdownPollingInterval, Duration debounceDuration) {
            this.mainClass = mainClass;
            this.packagePrefixes = packagePrefixes;
            this.classesOutputDirectories = classesOutputDirectories;
            this.shutdownPollingInterval = shutdownPollingInterval;
            this.debounceDuration = debounceDuration;
        }

        private static Configuration parse() {
            String mainClass = System.getProperty("zackbumm.mainClass");
            List<String> packagePrefixes = Arrays.stream(System.getProperty("zackbumm.packagePrefixes").split(",")).toList();
            Set<Path> classesOutputDirectories = Arrays.stream(System.getProperty("zackbumm.classesOutputs").split(File.pathSeparator))
                    .map(Path::of)
                    .collect(Collectors.toSet());
            Duration shutdownPollingInterval = parseDuration(System.getProperty("zackbumm.shutdownPollingInterval"), Duration.ofSeconds(5));
            Duration debounceDuration = parseDuration(System.getProperty("zackbumm.debounceDuration"), Duration.ofMillis(10));

            return new Configuration(
                    mainClass,
                    packagePrefixes,
                    classesOutputDirectories,
                    shutdownPollingInterval,
                    debounceDuration
            );
        }
    }


    public static void main(String[] args) throws IOException {
        // TODO: Pass args to main app (and verify it!)
        startDevMode(Configuration.parse());
    }

    public static void startDevMode(Configuration configuration) throws IOException {
        // TODO: Later -> Add support for jars (gradle modules)
        URL[] classPathUrls = configuration.classesOutputDirectories.stream()
                .map(DevMode::toUrl)
                .toArray(URL[]::new);

        List<PathMatcher> restartMatchers = configuration.classesOutputDirectories.stream()
                .map(Path::toAbsolutePath)
                .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it + "/**"))
                .toList();

        ApplicationLoader applicationLoader = new ApplicationLoader(
                configuration.mainClass,
                configuration.packagePrefixes,
                classPathUrls,
                configuration.shutdownPollingInterval
        );
        applicationLoader.start();

        PathUpdateDebouncer restartDebouncer = new PathUpdateDebouncer(configuration.debounceDuration, pathUpdates -> {
            logger.fine(() -> "Restarting due to " + pathUpdates);
            applicationLoader.restart();
        });

        new FileSystemWatcher(configuration.classesOutputDirectories, fileSystemEvent -> {
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

    private static Duration parseDuration(String property, Duration defaultValue) {
        return Optional.ofNullable(property)
                .map((it) -> Duration.ofMillis(Long.parseLong(it)))
                .orElse(defaultValue);
    }
}
