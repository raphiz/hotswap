package io.github.raphiz.hotswap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static io.github.raphiz.hotswap.GreeterAppWriter.CLASS_NAME;
import static io.github.raphiz.hotswap.GreeterAppWriter.PACKAGE_PREFIX;

class DevModeTest {
    public static final Duration SHUTDOWN_POLLING_INTERVAL = Duration.ofMillis(100);
    private final GreeterAppWriter greeterAppWriter = new GreeterAppWriter();
    private final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();

    @BeforeEach
    public void prepareLogger() {
        Stream.of(ApplicationLoader.class, DevMode.class).forEach(clazz -> {
            Logger logger = Logger.getLogger(clazz.getName());
            logger.setLevel(Level.ALL);
            logger.addHandler(capturingLogHandler);
        });
    }

    @Test
    void testApplicationLoaderRestartsApplication() throws Exception {
        // Compile initial program version
        greeterAppWriter.writeCodeWithMessage("Hello");
        greeterAppWriter.compile();

        // Start application
        DevMode.startDevMode(new DevMode.Configuration(
                PACKAGE_PREFIX + "." + CLASS_NAME,
                new String[]{"World", "Universe"},
                Set.of(PACKAGE_PREFIX),
                Set.of(greeterAppWriter.getBuildDirectory()),
                SHUTDOWN_POLLING_INTERVAL,
                Duration.ofMillis(20)
        ));

        // Wait for initial message
        greeterAppWriter.assertOutputsMessage("Hello World, Universe");
        capturingLogHandler.assertLogRecords(
                new LogRecord(Level.INFO, "Starting Application com.example.HelloWorldApp")
        );

        // Recompile - the watcher and debouncers should trigger exactly one reload
        greeterAppWriter.writeCodeWithMessage("Hi");
        greeterAppWriter.compile();

        // Wait for updated message
        greeterAppWriter.assertOutputsMessage("Hi World, Universe");

        capturingLogHandler.assertLogRecords(
                new LogRecord(Level.FINE, "Restarting due to PathUpdates[created=[], modified=[" + greeterAppWriter.getBuildDirectory() + "/com/example/HelloWorldApp.class], deleted=[]]"),
                new LogRecord(Level.INFO, "Restarting Application com.example.HelloWorldApp"),
                new LogRecord(Level.INFO, "Stopping Application com.example.HelloWorldApp"),
                new LogRecord(Level.INFO, "Interrupting existing application thread"),
                new LogRecord(Level.FINE, "Clean up previous class loader and application instance"),
                new LogRecord(Level.INFO, "Starting Application com.example.HelloWorldApp")
        );
    }

}