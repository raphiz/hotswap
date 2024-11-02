package io.github.raphiz.zackbumm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUpdatesTest {
    @Test
    void defaultConstructorInitializesEmptySets() {
        PathUpdates pathUpdates = new PathUpdates();

        assertTrue(pathUpdates.created().isEmpty());
        assertTrue(pathUpdates.modified().isEmpty());
        assertTrue(pathUpdates.deleted().isEmpty());
    }
}