package io.github.raphiz.zackbumm;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class DevMode {
    private static final Logger logger = LoggerHelpers.logger();

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
}
