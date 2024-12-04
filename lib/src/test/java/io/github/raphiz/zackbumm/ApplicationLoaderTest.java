package io.github.raphiz.zackbumm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLClassLoader;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.raphiz.zackbumm.GreeterAppWriter.CLASS_NAME;
import static io.github.raphiz.zackbumm.GreeterAppWriter.PACKAGE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationLoaderTest {

    public static final Duration SHUTDOWN_POLLING_INTERVAL = Duration.ofMillis(100);
    private final GreeterAppWriter greeterAppWriter = new GreeterAppWriter();
    private final URLClassLoader parentClassLoader = greeterAppWriter.createFakeParentClassLoader();
    private final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();
    private ApplicationLoader applicationLoader;


    @BeforeEach
    public void prepareLogger() {
        Logger logger = Logger.getLogger(ApplicationLoader.class.getName());
        logger.setLevel(Level.ALL);
        logger.addHandler(capturingLogHandler);
    }

    @Test
    void testApplicationLoaderRestartsApplication() throws Exception {
        // Compile initial program version
        greeterAppWriter.writeCodeWithMessage("Hello");
        greeterAppWriter.compile();

        // Start application via ApplicationLoader
        applicationLoader = new ApplicationLoader(
                PACKAGE_PREFIX + "." + CLASS_NAME,
                new String[]{"World", "Universe"},
                List.of(PACKAGE_PREFIX),
                parentClassLoader.getURLs(),
                SHUTDOWN_POLLING_INTERVAL
        );
        applicationLoader.start();
        greeterAppWriter.assertOutputsMessage("Hello World, Universe");
        capturingLogHandler.assertLogRecords(
                new LogRecord(Level.INFO, "Starting Application com.example.HelloWorldApp")
        );

        // Change the greeter message
        greeterAppWriter.writeCodeWithMessage("Hi");
        greeterAppWriter.compile();

        // Restart the application
        applicationLoader.restart();
        capturingLogHandler.assertLogRecords(
                new LogRecord(Level.INFO, "Restarting Application com.example.HelloWorldApp"),
                new LogRecord(Level.INFO, "Stopping Application com.example.HelloWorldApp"),
                new LogRecord(Level.INFO, "Interrupting existing application thread"),
                new LogRecord(Level.FINE, "Clean up previous class loader and application instance"),
                new LogRecord(Level.INFO, "Starting Application com.example.HelloWorldApp")
        );

        greeterAppWriter.assertOutputsMessage("Hi World, Universe");
    }

    @Test
    void testApplicationLoaderLogsWarningsForNonResponsiveApp() throws Exception {
        // Compile initial program version
        greeterAppWriter.writeAppWithSlowShutdownProcedure(SHUTDOWN_POLLING_INTERVAL.multipliedBy(2));
        greeterAppWriter.compile();

        // Start application via ApplicationLoader
        applicationLoader = new ApplicationLoader(
                PACKAGE_PREFIX + "." + CLASS_NAME,
                new String[]{},
                List.of(PACKAGE_PREFIX),
                parentClassLoader.getURLs(),
                SHUTDOWN_POLLING_INTERVAL
        );
        applicationLoader.start();

        // Prepare an updated version
        greeterAppWriter.writeCodeWithMessage("Hello Universe");
        greeterAppWriter.compile();

        // Try to restart the application
        capturingLogHandler.clear();
        applicationLoader.restart();

        // Ensure that the warning message is logged
        assertTrue(capturingLogHandler.getRecords().contains(
                new LogRecord(Level.WARNING, "Application thread is still running after interrupt.")
        ));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (applicationLoader != null) {
            applicationLoader.stop();  // In case it's still running
        }
        greeterAppWriter.close();
    }
}
