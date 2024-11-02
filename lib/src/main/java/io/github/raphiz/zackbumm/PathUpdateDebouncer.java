package io.github.raphiz.zackbumm;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PathUpdateDebouncer {
    private final Duration timeout;
    private final Consumer<PathUpdates> callback;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private PathUpdates pathUpdates;

    public PathUpdateDebouncer(Duration timeout, Consumer<PathUpdates> callback) {
        this.timeout = timeout;
        this.callback = callback;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.future = null;
        this.pathUpdates = new PathUpdates();
    }

    public synchronized void submit(Path path, EventType eventType) {
        pathUpdates = pathUpdates.withPathUpdate(path, eventType);

        if (future != null) {
            future.cancel(false);
        }

        future = executor.schedule(() -> {
            synchronized (this) {
                PathUpdates updates = pathUpdates;
                pathUpdates = new PathUpdates();
                future = null;
                callback.accept(updates);
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
