package io.github.raphiz.zackbum

import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class FileSystemWatcherTest {
    @Test
    fun `it notifies when a new file is created`() {
        val workspace = createTempDirectory()
        val file = workspace.resolve("create.txt")

        workspace.watchPerformAssertUntil(
            {
                file.writeText("Created")
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactlyCreatedEventsFor(file)
            },
        )
    }

    @Test
    fun `it notifies when a new file is created in a subdirectory`() {
        val workspace = createTempDirectory()
        val directory = workspace.resolve("directory").createDirectory()
        val file = directory.resolve("create.txt")

        workspace.watchPerformAssertUntil(
            {
                file.writeText("Created")
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactlyCreatedEventsFor(file)
            },
        )
    }

    @Test
    fun `it does not notify for files in hidden directories`() {
        val workspace = createTempDirectory()
        val hidden = workspace.resolve(".hidden").createDirectory()
        val subHidden = hidden.resolve("sub").createDirectory()
        val rootFile = workspace.resolve("create.txt")

        workspace.watchPerformAssertUntil(
            {
                subHidden.resolve("example.txt").createFile()
                hidden.resolve("example.txt").createFile()
                rootFile.writeText("Created")
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactlyCreatedEventsFor(rootFile)
            },
        )
    }

    @Test
    fun `it notifies when a file is created in a new subdirectory`() {
        val workspace = createTempDirectory()
        val directory = workspace.resolve("directory")
        val subDirectory = directory.resolve("subDirectory")
        val file = subDirectory.resolve("create.txt")

        workspace.watchPerformAssertUntil(
            {
                directory.createDirectory()
                subDirectory.createDirectory()
                file.writeText("Created")
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactlyCreatedEventsFor(file)
            },
        )
    }

    @Test
    fun `it notifies when a file is changed`() {
        val workspace = createTempDirectory()
        val file = workspace.resolve("change.txt")
        file.writeText("Created")

        workspace.watchPerformAssertUntil(
            {
                file.writeText("changed")
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactly(Pair(file, EventType.MODIFIED))
            },
        )
    }

    @Test
    fun `it notifies when a file is deleted`() {
        val workspace = createTempDirectory()
        val file = workspace.resolve("delete.txt")
        file.writeText("Created")

        workspace.watchPerformAssertUntil(
            {
                file.deleteIfExists()
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactly(Pair(file, EventType.DELETED))
            },
        )
    }

    @Test
    fun `it notifies when a directory is deleted`() {
        val workspace = createTempDirectory()
        val directory = workspace.resolve("delete").createDirectory()

        workspace.watchPerformAssertUntil(
            {
                directory.deleteIfExists()
            },
            { changedPaths ->
                expectThat(changedPaths).containsExactly(Pair(directory, EventType.DELETED))
            },
        )
    }

    private fun Path.watchPerformAssertUntil(
        action: () -> Unit,
        assertion: (Set<Pair<Path, EventType>>) -> Unit,
    ) {
        val changedPaths = mutableListOf<Pair<Path, EventType>>()

        val watcher =
            Watcher(this) { path, eventType ->
                changedPaths.add(Pair(path, eventType))
            }
        watcher.start()
        action()

        waitUntil { assertion(changedPaths.toSet()) }
        watcher.stop()
    }

    private fun waitUntil(assertion: () -> Unit) {
        Awaitility
            .await()
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .atMost(250, TimeUnit.MILLISECONDS)
            .untilAsserted(assertion)
    }
}

private fun Assertion.Builder<Set<Pair<Path, EventType>>>.containsExactlyCreatedEventsFor(file: Path) =
    if (subject.size > 1) {
        expectThat(subject).containsExactly(
            Pair(file, EventType.CREATED),
            Pair(file, EventType.MODIFIED), // Modified event is optional
        )
    } else {
        expectThat(subject).containsExactly(Pair(file, EventType.CREATED))
    }
