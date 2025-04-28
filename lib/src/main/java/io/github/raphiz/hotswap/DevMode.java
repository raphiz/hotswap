package io.github.raphiz.hotswap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DevMode {
    private static final Logger logger = LoggerHelpers.logger();

    public static class Configuration {
        final String mainClass;
        final String[] args;
        final Set<String> packagePrefixes;
        final Set<Path> classPath;
        final Duration shutdownPollingInterval;
        final Duration debounceDuration;

        Configuration(String mainClass, String[] args, Set<String> packagePrefixes, Set<Path> classPath, Duration shutdownPollingInterval, Duration debounceDuration) {
            this.mainClass = mainClass;
            this.args = args;
            this.packagePrefixes = packagePrefixes;
            this.classPath = classPath;
            this.shutdownPollingInterval = shutdownPollingInterval;
            this.debounceDuration = debounceDuration;
        }

        public static Configuration parse(Map<String, String> properties, String[] args) {
            String mainClass = Objects.requireNonNull(properties.get("hotswap.mainClass"), "Main class must be provided");

            Set<Path> classPath = Arrays.stream(properties.getOrDefault("hotswap.classPath", System.getProperty("java.class.path")).split(File.pathSeparator))
                    .filter((it) -> !it.isBlank())
                    .map(Path::of)
                    .collect(Collectors.toSet());

            if (classPath.isEmpty()) {
                throw new IllegalArgumentException("At least one output directory to watch must be provided");
            }

            Set<String> packagePrefixes;
            if (properties.get("hotswap.packagePrefixes") != null) {
                packagePrefixes = Arrays.stream(properties.get("hotswap.packagePrefixes").split(","))
                        .filter((it) -> !it.isBlank())
                        .collect(Collectors.toSet());
                if (packagePrefixes.isEmpty()) {
                    packagePrefixes = null;
                }
            } else {
                packagePrefixes = null;
            }
            Duration shutdownPollingInterval = parseDuration(emptyToNull(properties.get("hotswap.shutdownPollingInterval")), Duration.ofSeconds(5));
            Duration debounceDuration = parseDuration(emptyToNull(properties.get("hotswap.debounceDuration")), Duration.ofMillis(100));

            return new Configuration(
                    mainClass,
                    args,
                    packagePrefixes,
                    classPath,
                    shutdownPollingInterval,
                    debounceDuration
            );
        }

        private static String emptyToNull(String value) {
            return (value == null || value.isEmpty()) ? null : value;
        }
    }


    public static void main(String[] args) throws IOException {
        Map<String, String> systemProperties = System.getProperties().entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        Configuration configuration = Configuration.parse(systemProperties, args);
        startDevMode(configuration);
    }

    public static void startDevMode(Configuration configuration) throws IOException {
        // TODO: Later -> Add support for jars (gradle modules)
        URL[] classPathUrls = configuration.classPath.stream()
                .map(DevMode::toUrl)
                .toArray(URL[]::new);

        List<PathMatcher> restartMatchers = configuration.classPath.stream()
                .map(Path::toAbsolutePath)
                .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it + "/**"))
                .toList();

        ApplicationLoader applicationLoader = new ApplicationLoader(
                configuration.mainClass,
                configuration.args,
                configuration.packagePrefixes,
                classPathUrls,
                configuration.shutdownPollingInterval
        );
        applicationLoader.start();

        PathUpdateDebouncer restartDebouncer = new PathUpdateDebouncer(configuration.debounceDuration, pathUpdates -> {
            logger.fine(() -> "Restarting due to " + pathUpdates);
            applicationLoader.restart();
        });

        new FileSystemWatcher(configuration.classPath, fileSystemEvent -> {
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
