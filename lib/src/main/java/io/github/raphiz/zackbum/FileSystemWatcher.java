package io.github.raphiz.zackbum;

import java.nio.file.Path;

enum EventType {
    CREATED,
    MODIFIED,
    DELETED,
}

record FileSystemEvent(Path path, EventType eventType) {
}
