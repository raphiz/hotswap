package io.github.raphiz.zackbum

import io.github.raphiz.zackbum.LoggerHelpers.logger
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden

internal class FileSystemWatcher(
    private val workspace: Path,
    private val onChange: (FileSystemEvent) -> Unit,
) {
    private val logger = logger()
    private val active = AtomicBoolean(false)

    private val watchService = FileSystems.getDefault().newWatchService()

    private fun Path.watchRecursively(notify: Boolean = false) {
        Files.walkFileTree(
            this,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (notify) {
                        onChange(FileSystemEvent(file, EventType.CREATED))
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult =
                    if (dir.isDirectory() && !dir.isHidden()) {
                        dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                        )
                        FileVisitResult.CONTINUE
                    } else {
                        FileVisitResult.SKIP_SUBTREE
                    }
            },
        )
    }

    fun start() {
        logger.fine { "Starting watch service on workspace ${workspace.toAbsolutePath()}" }
        workspace.watchRecursively()
        active.set(true)
        Executors.newSingleThreadExecutor().submit {
            Thread.currentThread().setName("filewatch-thread")
            while (active.get()) {
                val watchKey = watchService.poll(300, TimeUnit.MILLISECONDS) ?: continue
                val directory = watchKey.watchable() as Path
                watchKey.pollEvents().forEach { event ->
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        logger.warning("WatchService Overflow occurred")
                    } else {
                        val changedFile = directory.resolve(event.context() as Path)
                        if (changedFile.isDirectory()) {
                            changedFile.watchRecursively(notify = true)
                        } else {
                            logger.fine { "Received event ${event.kind().name()} for file $changedFile" }
                            onChange(FileSystemEvent(changedFile, event.toEventType()))
                        }
                    }
                }

                // reset the key to watch for future events
                val keyIsValid = watchKey.reset()
                if (!keyIsValid) {
                    logger.fine { "$directory has been unregistered" }
                    if (directory == workspace) {
                        logger.info("Workspace has been unregistered. Will stop watch service")
                        // if the workspace directory is not valid anymore it was either deleted or the watchService was stopped
                        // in either case, this watcher is done with its work
                        active.set(false)
                    }
                }
            }
        }
    }

    fun stop() {
        logger.info("Stopping watch service on workspace $workspace")
        active.set(false)
        watchService.close()
    }
}

enum class EventType {
    CREATED,
    MODIFIED,
    DELETED,
}

data class FileSystemEvent(
    val path: Path,
    val eventType: EventType,
)

private fun WatchEvent<*>.toEventType() =
    when (kind()) {
        StandardWatchEventKinds.ENTRY_MODIFY -> EventType.MODIFIED
        StandardWatchEventKinds.ENTRY_DELETE -> EventType.DELETED
        StandardWatchEventKinds.ENTRY_CREATE -> EventType.CREATED
        else -> throw IllegalStateException("Unknown event kind ${kind()} received")
    }
