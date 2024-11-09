package io.github.raphiz.zackbumm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationLoaderTest {

    private static final String PACKAGE_PREFIX = "com.example";
    private static final String CLASS_NAME = "HelloWorldApp";
    public static final Duration SHUTDOWN_POLLING_INTERVAL = Duration.ofMillis(100);
    private final GreeterAppWriter greeterAppWriter = new GreeterAppWriter(PACKAGE_PREFIX, CLASS_NAME);
    private final URLClassLoader parentClassLoader = createFakeParentClassLoader();
    private final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();
    private ApplicationLoader applicationLoader;


    @BeforeEach
    public void prepareLogger() {
        Logger logger = Logger.getLogger(ApplicationLoader.class.getName());
        logger.addHandler(capturingLogHandler);
    }

    @Test
    void testApplicationLoaderRestartsApplication() throws Exception {
        // Compile initial program version
        greeterAppWriter.writeCodeWithMessage("Hello World");
        greeterAppWriter.compile();

        // Start application via ApplicationLoader
        applicationLoader = new ApplicationLoader(
                PACKAGE_PREFIX + "." + CLASS_NAME,
                List.of(PACKAGE_PREFIX),
                parentClassLoader.getURLs(),
                SHUTDOWN_POLLING_INTERVAL
        );
        applicationLoader.start();
        greeterAppWriter.assertLoggedMessage("Hello World");
        capturingLogHandler.assertLogRecords(
                new LogRecord(Level.INFO, "Starting Application com.example.HelloWorldApp")
        );

        // Change the greeter message
        greeterAppWriter.writeCodeWithMessage("Hello Universe");
        greeterAppWriter.compile();

        // Restart the application
        applicationLoader.restart();
        capturingLogHandler.assertLogRecords(
                new LogRecord(Level.INFO, "Restarting Application com.example.HelloWorldApp"),
                new LogRecord(Level.INFO, "Stopping Application com.example.HelloWorldApp"),
                new LogRecord(Level.INFO, "Interrupting existing application thread"),
                new LogRecord(Level.INFO, "Starting Application com.example.HelloWorldApp")
        );

        greeterAppWriter.assertLoggedMessage("Hello Universe");         // TODO: Renae assertOutputsMessage
    }

    @Test
    void testApplicationLoaderLogsWarningsForNonResponsiveApp() throws Exception {
        // Compile initial program version
        greeterAppWriter.writeAppWithSlowShutdownProcedure(SHUTDOWN_POLLING_INTERVAL.multipliedBy(2));
        greeterAppWriter.compile();

        // Start application via ApplicationLoader
        applicationLoader = new ApplicationLoader(
                PACKAGE_PREFIX + "." + CLASS_NAME,
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

    private URLClassLoader createFakeParentClassLoader() {
        try {
            return new URLClassLoader(new URL[]{greeterAppWriter.getBuildDirectory().toUri().toURL()}, ClassLoader.getSystemClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


}
