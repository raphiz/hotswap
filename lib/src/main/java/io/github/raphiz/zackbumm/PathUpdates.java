package io.github.raphiz.zackbumm;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record PathUpdates(
        Set<Path> created,
        Set<Path> modified,
        Set<Path> deleted
) {
    public PathUpdates() {
        this(Set.of(), Set.of(), Set.of());
    }

    public PathUpdates withCreated(Path path) {
        return new PathUpdates(
                plusElement(created, path),
                minusElement(modified, path),
                minusElement(deleted, path)
        );
    }

    public PathUpdates withModified(Path path) {
        if (created.contains(path)) {
            return this;
        } else {
            return new PathUpdates(
                    created,
                    plusElement(modified, path),
                    minusElement(deleted, path)
            );
        }
    }

    public PathUpdates withDeleted(Path path) {
        return new PathUpdates(
                minusElement(created, path),
                minusElement(modified, path),
                plusElement(deleted, path)
        );
    }

    private static Set<Path> plusElement(Set<Path> set, Path element) {
        Set<Path> newSet = new HashSet<>(set);
        newSet.add(element);
        return Collections.unmodifiableSet(newSet);
    }

    private static Set<Path> minusElement(Set<Path> set, Path element) {
        Set<Path> newSet = new HashSet<>(set);
        newSet.remove(element);
        return Collections.unmodifiableSet(newSet);
    }
}
