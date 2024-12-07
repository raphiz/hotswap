package io.github.raphiz.hotswap;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSystemFileSystemWatcherTest {

    @Test
    public void itNotifiesWhenANewFileIsCreated() throws Exception {
        Set<Path> workspaces = manyWorkspaces();
        Path file = random(workspaces).resolve("create.txt");

        performActionAndAssertEvents(
                workspaces,
                () -> Files.writeString(file, "Created"),
                changedPaths -> containsExactlyCreatedEventsFor(changedPaths, file)
        );
    }

    @Test
    public void itNotifiesWhenANewFileIsCreatedInASubdirectory() throws Exception {
        Set<Path> workspaces = manyWorkspaces();
        Path directory = Files.createDirectory(random(workspaces).resolve("directory"));
        Path file = directory.resolve("create.txt");

        performActionAndAssertEvents(
                workspaces,
                () -> Files.writeString(file, "Created"),
                changedPaths -> containsExactlyCreatedEventsFor(changedPaths, file)
        );
    }

    @Test
    public void itDoesNotNotifyForFilesInHiddenDirectories() throws Exception {
        Set<Path> workspaces = manyWorkspaces();
        Path hidden = Files.createDirectory(random(workspaces).resolve(".hidden"));
        Path subHidden = Files.createDirectory(hidden.resolve("sub"));
        Path rootFile = random(workspaces).resolve("create.txt");

        performActionAndAssertEvents(
                workspaces,
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
        Set<Path> workspaces = manyWorkspaces();
        Path directory = random(workspaces).resolve("directory");
        Path subDirectory = directory.resolve("subDirectory");
        Path file = subDirectory.resolve("create.txt");

        performActionAndAssertEvents(
                workspaces,
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
        Set<Path> workspaces = manyWorkspaces();
        Path file = random(workspaces).resolve("change.txt");
        Files.writeString(file, "Created");

        performActionAndAssertEvents(
                workspaces,
                () -> Files.writeString(file, "changed"),
                changedPaths -> assertEquals(
                        Set.of(new FileSystemEvent(file, EventType.MODIFIED)),
                        changedPaths
                )
        );
    }

    @Test
    public void itNotifiesWhenAFileIsDeleted() throws Exception {
        Set<Path> workspaces = manyWorkspaces();
        Path file = random(workspaces).resolve("delete.txt");
        Files.writeString(file, "Created");

        performActionAndAssertEvents(
                workspaces,
                () -> Files.deleteIfExists(file),
                changedPaths -> assertEquals(
                        Set.of(new FileSystemEvent(file, EventType.DELETED)),
                        changedPaths
                )
        );
    }

    @Test
    public void itNotifiesWhenADirectoryIsDeleted() throws Exception {
        Set<Path> workspaces = manyWorkspaces();
        Path directory = Files.createDirectory(random(workspaces).resolve("delete"));

        performActionAndAssertEvents(
                workspaces,
                () -> Files.deleteIfExists(directory),
                changedPaths -> assertEquals(
                        Collections.singleton(new FileSystemEvent(directory, EventType.DELETED)),
                        changedPaths
                )
        );
    }

    @Test
    void itIgnoresNonExistingDirectories() throws Exception {
        Set<Path> workspaces = manyWorkspaces();
        Path file = random(workspaces).resolve("delete.txt");
        workspaces.add(Path.of("/does/not/exist"));

        performActionAndAssertEvents(
                new HashSet<>(workspaces),
                () -> Files.createFile(file),
                changedPaths -> assertEquals(
                        Collections.singleton(new FileSystemEvent(file, EventType.CREATED)),
                        changedPaths
                )
        );


    }

    private void performActionAndAssertEvents(
            Set<Path> workspaces,
            ThrowingRunnable action,
            Consumer<Set<FileSystemEvent>> assertion
    ) throws Exception {
        Set<FileSystemEvent> changedPaths = new HashSet<>();
        FileSystemWatcher fileSystemWatcher = new FileSystemWatcher(workspaces, changedPaths::add);
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

    private Path random(Set<Path> workspaces) {
        int randomIndex = ThreadLocalRandom.current().nextInt(workspaces.size());
        return new ArrayList<>(workspaces).get(randomIndex);
    }

    private Set<Path> manyWorkspaces() throws IOException {
        Set<Path> paths = new HashSet<>();
        paths.add(Files.createTempDirectory(null));
        paths.add(Files.createTempDirectory(null));
        return paths;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
