package io.github.raphiz.zackbumm;

import java.nio.file.Path;
import java.util.Set;

public record PathUpdates(
        Set<Path> created,
        Set<Path> modified,
        Set<Path> deleted
) {
    public PathUpdates() {
        this(Set.of(), Set.of(), Set.of());
    }
}
