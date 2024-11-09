package io.github.raphiz.zackbumm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CapturingLogHandler extends Handler {
    private final List<String> logRecords = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void publish(LogRecord record) {
        logRecords.add(record.getMessage());
    }

    @Override
    public void flush() {
        // Not needed for tests
    }

    @Override
    public void close() throws SecurityException {
        logRecords.clear();
    }

    List<String> getRecords() {
        return List.copyOf(logRecords);
    }

    void clear() {
        logRecords.clear();
    }

    void assertLogMessages(String... messages) {
        assertEquals(List.of(messages), getRecords());
        clear();
    }
}