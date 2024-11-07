package io.github.raphiz.zackbumm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class ApplicationLoaderTest {

    private static final String PACKAGE_PREFIX = "com.example";
    private static final String CLASS_NAME = "HelloWorldApp";
    private final GreeterAppWriter greeterAppWriter = new GreeterAppWriter(PACKAGE_PREFIX, CLASS_NAME);
    private final URLClassLoader parentClassLoader = createFakeParentClassLoader();
    private ApplicationLoader applicationLoader;


    @Test
    void testApplicationLoaderRestartsApplication() throws Exception {
        // Compile initial program version
        greeterAppWriter.writeCodeWithMessage("Hello World");
        greeterAppWriter.compile();

        // Start application via ApplicationLoader
        applicationLoader = new ApplicationLoader(
                PACKAGE_PREFIX + "." + CLASS_NAME,
                List.of(PACKAGE_PREFIX),
                parentClassLoader.getURLs()
        );
        applicationLoader.start();
        greeterAppWriter.assertLoggedMessage("Hello World");

        // Change the greeter message
        greeterAppWriter.writeCodeWithMessage("Hello Universe");
        greeterAppWriter.compile();

        // Restart the application
        applicationLoader.restart();
        greeterAppWriter.assertLoggedMessage("Hello Universe");
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
