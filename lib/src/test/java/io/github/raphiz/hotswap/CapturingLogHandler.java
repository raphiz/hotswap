package io.github.raphiz.hotswap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;

record LogRecord(Level level, String message) {
}

class CapturingLogHandler extends Handler {
    private final List<LogRecord> logRecords = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void publish(java.util.logging.LogRecord record) {
        logRecords.add(new LogRecord(record.getLevel(), record.getMessage()));
    }

    @Override
    public void flush() {
        // Not needed for tests
    }

    @Override
    public void close() throws SecurityException {
        logRecords.clear();
    }

    List<LogRecord> getRecords() {
        return List.copyOf(logRecords);
    }

    void clear() {
        logRecords.clear();
    }

    void assertLogRecords(LogRecord... messages) {
        assertEquals(List.of(messages), getRecords());
        clear();
    }
}