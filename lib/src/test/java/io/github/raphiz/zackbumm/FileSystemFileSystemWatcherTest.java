package io.github.raphiz.zackbumm;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSystemFileSystemWatcherTest {

    @Test
    public void itNotifiesWhenANewFileIsCreated() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path file = workspace.resolve("create.txt");

        performActionAndAssertEvents(
                workspace,
                () -> Files.writeString(file, "Created"),
                changedPaths -> containsExactlyCreatedEventsFor(changedPaths, file)
        );
    }

    @Test
    public void itNotifiesWhenANewFileIsCreatedInASubdirectory() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path directory = Files.createDirectory(workspace.resolve("directory"));
        Path file = directory.resolve("create.txt");

        performActionAndAssertEvents(
                workspace,
                () -> Files.writeString(file, "Created"),
                changedPaths -> containsExactlyCreatedEventsFor(changedPaths, file)
        );
    }

    @Test
    public void itDoesNotNotifyForFilesInHiddenDirectories() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path hidden = Files.createDirectory(workspace.resolve(".hidden"));
        Path subHidden = Files.createDirectory(hidden.resolve("sub"));
        Path rootFile = workspace.resolve("create.txt");

        performActionAndAssertEvents(
                workspace,
                () -> {
                    Files.createFile(subHidden.resolve("example.txt"));
                    Files.createFile(hidden.resolve("example.txt"));
                    Files.writeString(rootFile, "Created");
                },
                changedPaths -> containsExactlyCreatedEventsFor(changedPaths, rootFile)
        );
    }

    @Test
    public void itNotifiesWhenAFileIsCreatedInANewSubdirectory() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path directory = workspace.resolve("directory");
        Path subDirectory = directory.resolve("subDirectory");
        Path file = subDirectory.resolve("create.txt");

        performActionAndAssertEvents(
                workspace,
                () -> {
                    Files.createDirectory(directory);
                    Files.createDirectory(subDirectory);
                    Files.writeString(file, "Created");
                },
                changedPaths -> containsExactlyCreatedEventsFor(changedPaths, file)
        );
    }

    @Test
    public void itNotifiesWhenAFileIsChanged() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path file = workspace.resolve("change.txt");
        Files.writeString(file, "Created");

        performActionAndAssertEvents(
                workspace,
                () -> Files.writeString(file, "changed"),
                changedPaths -> assertEquals(
                        Set.of(new FileSystemEvent(file, EventType.MODIFIED)),
                        changedPaths
                )
        );
    }

    @Test
    public void itNotifiesWhenAFileIsDeleted() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path file = workspace.resolve("delete.txt");
        Files.writeString(file, "Created");

        performActionAndAssertEvents(
                workspace,
                () -> Files.deleteIfExists(file),
                changedPaths -> assertEquals(
                        Set.of(new FileSystemEvent(file, EventType.DELETED)),
                        changedPaths
                )
        );
    }

    @Test
    public void itNotifiesWhenADirectoryIsDeleted() throws Exception {
        Path workspace = Files.createTempDirectory(null);
        Path directory = Files.createDirectory(workspace.resolve("delete"));

        performActionAndAssertEvents(
                workspace,
                () -> Files.deleteIfExists(directory),
                changedPaths -> assertEquals(
                        Collections.singleton(new FileSystemEvent(directory, EventType.DELETED)),
                        changedPaths
                )
        );
    }

    private void performActionAndAssertEvents(
            Path workspace,
            ThrowingRunnable action,
            Consumer<Set<FileSystemEvent>> assertion
    ) throws Exception {
        Set<FileSystemEvent> changedPaths = new HashSet<>();
        FileSystemWatcher fileSystemWatcher = new FileSystemWatcher(workspace, changedPaths::add);
        fileSystemWatcher.start();

        action.run();

        waitUntil(() -> assertion.accept(changedPaths));
        fileSystemWatcher.stop();
    }

    private void waitUntil(ThrowingRunnable assertion) {
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(250, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    try {
                        assertion.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void containsExactlyCreatedEventsFor(Set<FileSystemEvent> changedPaths, Path file) {
        Set<FileSystemEvent> expectedEvents;
        if (changedPaths.size() > 1) {
            expectedEvents = new HashSet<>(Arrays.asList(
                    new FileSystemEvent(file, EventType.CREATED),
                    new FileSystemEvent(file, EventType.MODIFIED) // Modified event is optional
            ));
        } else {
            expectedEvents = Collections.singleton(new FileSystemEvent(file, EventType.CREATED));
        }
        assertEquals(expectedEvents, changedPaths);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
