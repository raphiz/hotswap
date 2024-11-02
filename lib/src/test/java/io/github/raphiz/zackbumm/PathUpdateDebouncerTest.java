package io.github.raphiz.zackbumm;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUpdateDebouncerTest {
    private static final Duration TIMEOUT = Duration.ofMillis(100);

    @Test
    void callbackIsCalledWithAggregatedUpdatesAfterTimeout() {
        var callbackInvocations = new CopyOnWriteArrayList<PathUpdates>();
        var debouncer = new PathUpdateDebouncer(TIMEOUT, callbackInvocations::add);
        var createdPath = Path.of("/path/to/file1");
        var modifiedPath = Path.of("/path/to/file2");

        debouncer.submit(createdPath, EventType.CREATED);
        debouncer.submit(modifiedPath, EventType.MODIFIED);

        await().untilAsserted(() -> {
            assertEquals(1, callbackInvocations.size());

            PathUpdates actualPathUpdates = callbackInvocations.get(0);
            PathUpdates expectedPathUpdates = new PathUpdates(Set.of(createdPath), Set.of(modifiedPath), Set.of());
            assertEquals(expectedPathUpdates, actualPathUpdates);
        });
    }

    @Test
    void consecutiveSubmissionsWithinTimeoutDelayCallbackExecution() throws InterruptedException {
        var callbackInvocations = new CopyOnWriteArrayList<PathUpdates>();
        var debouncer = new PathUpdateDebouncer(TIMEOUT, callbackInvocations::add);
        var createdPath = Path.of("/path/to/file1");
        var modifiedPath = Path.of("/path/to/file2");

        debouncer.submit(createdPath, EventType.CREATED);
        Thread.sleep(TIMEOUT.toMillis() / 2);
        debouncer.submit(modifiedPath, EventType.MODIFIED);

        await().untilAsserted(() -> {
            assertEquals(1, callbackInvocations.size());
            assertEquals(new PathUpdates(Set.of(createdPath), Set.of(modifiedPath), Set.of()), callbackInvocations.get(0));
        });
    }

    @Test
    void callbackIsNotCalledImmediatelyOnSubmit() throws InterruptedException {
        var callbackInvocations = new CopyOnWriteArrayList<Instant>();
        var debouncer = new PathUpdateDebouncer(TIMEOUT, (pathUpdates) -> callbackInvocations.add(Instant.now()));

        var submitTime = Instant.now();
        debouncer.submit(Path.of("/path/to/file"), EventType.CREATED);

        // Wait a bit longer than timeout
        Thread.sleep(TIMEOUT.toMillis() + 50);

        assertEquals(1, callbackInvocations.size());
        Duration actualTimeout = Duration.between(submitTime, callbackInvocations.get(0));
        assertTrue(actualTimeout.toMillis() >= TIMEOUT.toMillis());
    }

    @Test
    void callbackIsCalledMultipleTimesIfTimeoutsElapsedBetweenSubmissions() {
        var callbackInvocations = new CopyOnWriteArrayList<PathUpdates>();
        var debouncer = new PathUpdateDebouncer(TIMEOUT, callbackInvocations::add);
        Path createdPath = Path.of("/path/to/file1");
        Path modifiedPath = Path.of("/path/to/file2");

        debouncer.submit(createdPath, EventType.CREATED);
        await().untilAsserted(() -> assertEquals(1, callbackInvocations.size()));

        debouncer.submit(modifiedPath, EventType.MODIFIED);
        await().untilAsserted(() -> assertEquals(2, callbackInvocations.size()));

        assertEquals(new PathUpdates(Set.of(createdPath), Set.of(), Set.of()), callbackInvocations.get(0));
        assertEquals(new PathUpdates(Set.of(), Set.of(modifiedPath), Set.of()), callbackInvocations.get(1));
    }

    @Test
    void callbackIsNotCalledIfNoSubmissionsAfterInitialTimeout() throws InterruptedException {
        var callbackInvocations = new CopyOnWriteArrayList<PathUpdates>();
        new PathUpdateDebouncer(TIMEOUT, callbackInvocations::add);

        // Wait past the timeout duration without any submission
        Thread.sleep(TIMEOUT.toMillis() + 50);

        // Verify callback was never called since no submissions were made
        assertEquals(0, callbackInvocations.size());
    }
}