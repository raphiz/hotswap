package io.github.raphiz.zackbumm;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUpdatesTest {

    @Test
    void withPathUpdateAddsPathToCreatedSetWhenEventTypeIsCreated() {
        PathUpdates emptyPathUpdates = new PathUpdates();
        Path aPath = Path.of("/path/to/something");

        PathUpdates actualPathUpdates = emptyPathUpdates.withPathUpdate(aPath, EventType.CREATED);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(aPath), Set.of(), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withPathUpdateAddsPathToModifiedSetWhenEventTypeIsModified() {
        PathUpdates emptyPathUpdates = new PathUpdates();
        Path aPath = Path.of("/path/to/something");

        PathUpdates actualPathUpdates = emptyPathUpdates.withPathUpdate(aPath, EventType.MODIFIED);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(aPath), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withPathUpdateAddsPathToDeletedSetWhenEventTypeIsDeleted() {
        PathUpdates emptyPathUpdates = new PathUpdates();
        Path aPath = Path.of("/path/to/something");

        PathUpdates actualPathUpdates = emptyPathUpdates.withPathUpdate(aPath, EventType.DELETED);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(), Set.of(aPath));
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void defaultConstructorInitializesEmptySets() {
        PathUpdates pathUpdates = new PathUpdates();

        assertTrue(pathUpdates.created().isEmpty());
        assertTrue(pathUpdates.modified().isEmpty());
        assertTrue(pathUpdates.deleted().isEmpty());
    }

    @Test
    void withCreatedAddsPathToCreatedSet() {
        PathUpdates emptyPathUpdates = new PathUpdates();
        Path aPath = Path.of("/path/to/something");

        PathUpdates actualPathUpdates = emptyPathUpdates.withCreated(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(aPath), Set.of(), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withCreatedRemovesPathFromModifiedIfPresent() {
        Path aPath = Path.of("/path/to/something");
        PathUpdates pathUpdatesWithModified = new PathUpdates(Set.of(), Set.of(aPath), Set.of());

        PathUpdates actualPathUpdates = pathUpdatesWithModified.withCreated(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(aPath), Set.of(), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withCreatedRemovesPathFromDeletedIfPresent() {
        Path aPath = Path.of("/path/to/something");
        PathUpdates pathUpdatesWithModified = new PathUpdates(Set.of(), Set.of(), Set.of(aPath));

        PathUpdates actualPathUpdates = pathUpdatesWithModified.withCreated(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(aPath), Set.of(), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withModifiedAddsPathToModifiedSet() {
        PathUpdates emptyPathUpdates = new PathUpdates();
        Path aPath = Path.of("/path/to/something");

        PathUpdates actualPathUpdates = emptyPathUpdates.withModified(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(aPath), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withModifiedDoesNotAddPathToModifiedIfInCreated() {
        Path aPath = Path.of("/path/to/something");
        PathUpdates pathUpdatesWithCreated = new PathUpdates(Set.of(aPath), Set.of(), Set.of());

        PathUpdates actualPathUpdates = pathUpdatesWithCreated.withModified(aPath);

        // No changes expected
        assertEquals(pathUpdatesWithCreated, actualPathUpdates);
    }

    @Test
    void withModifiedDoesRemoveFromDeleted() {
        Path aPath = Path.of("/path/to/something");
        PathUpdates pathUpdatesWithDeleted = new PathUpdates(Set.of(), Set.of(), Set.of(aPath));

        PathUpdates actualPathUpdates = pathUpdatesWithDeleted.withModified(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(aPath), Set.of());
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withDeletedAddsPathToDeleted() {
        PathUpdates emptyPathUpdates = new PathUpdates();
        Path aPath = Path.of("/path/to/something");

        PathUpdates actualPathUpdates = emptyPathUpdates.withDeleted(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(), Set.of(aPath));
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withDeletedRemovesPathFromCreatedIfPresent() {
        Path aPath = Path.of("/path/to/something");
        PathUpdates pathUpdatesWithCreated = new PathUpdates(Set.of(aPath), Set.of(), Set.of());

        PathUpdates actualPathUpdates = pathUpdatesWithCreated.withDeleted(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(), Set.of(aPath));
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

    @Test
    void withDeletedRemovesPathFromModifiedIfPresent() {
        Path aPath = Path.of("/path/to/something");
        PathUpdates pathUpdatesWithModified = new PathUpdates(Set.of(), Set.of(aPath), Set.of());

        PathUpdates actualPathUpdates = pathUpdatesWithModified.withDeleted(aPath);

        PathUpdates expectedPathUpdates = new PathUpdates(Set.of(), Set.of(), Set.of(aPath));
        assertEquals(expectedPathUpdates, actualPathUpdates);
    }

}