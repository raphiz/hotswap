package io.github.raphiz.zackbumm;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

class FileSystemWatcher {
    private final Logger logger = LoggerHelpers.logger();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final Path workspace;
    private final Consumer<FileSystemEvent> onChange;
    private final WatchService watchService;

    public FileSystemWatcher(Path workspace, Consumer<FileSystemEvent> onChange) throws IOException {
        this.workspace = workspace;
        this.onChange = onChange;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    private void watchRecursively(Path path, boolean notify) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (notify) {
                    onChange.accept(new FileSystemEvent(file.toAbsolutePath(), EventType.CREATED));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!Files.isHidden(dir)) {
                    dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        });
    }

    public void start() throws IOException {
        logger.info("Starting watch service on workspace " + workspace.toAbsolutePath());
        watchRecursively(workspace, false);
        active.set(true);
        Executors.newSingleThreadExecutor().submit(() -> {
            Thread.currentThread().setName("filewatch-thread");
            while (active.get()) {
                WatchKey watchKey;
                try {
                    watchKey = watchService.poll(300, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (watchKey == null) {
                    continue;
                }
                Path directory = (Path) watchKey.watchable();
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        logger.warning("WatchService Overflow occurred");
                    } else {
                        Path changedFile = directory.resolve((Path) event.context());
                        if (Files.isDirectory(changedFile)) {
                            try {
                                watchRecursively(changedFile, true);
                            } catch (IOException e) {
                                logger.warning("Failed to watch directory " + changedFile + ": " + e.getMessage());
                            }
                        } else {
                            logger.fine(() -> "Received event " + event.kind().name() + " for file " + changedFile);
                            onChange.accept(new FileSystemEvent(changedFile.toAbsolutePath(), toEventType(event)));
                        }
                    }
                }

                boolean keyIsValid = watchKey.reset();
                if (!keyIsValid) {
                    logger.fine(() -> directory + " has been unregistered");
                    if (directory.equals(workspace)) {
                        logger.info("Workspace has been unregistered. Will stop watch service");
                        try {
                            stop();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });
    }

    public void stop() throws IOException {
        logger.info("Stopping watch service on workspace " + workspace);
        active.set(false);
        watchService.close();
    }

    private EventType toEventType(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return EventType.MODIFIED;
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return EventType.DELETED;
        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return EventType.CREATED;
        } else {
            throw new IllegalStateException("Unknown event kind " + kind + " received");
        }
    }
}

record FileSystemEvent(Path path, EventType eventType) {
}
