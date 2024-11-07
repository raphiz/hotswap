package io.github.raphiz.zackbumm;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;

public class ApplicationLoader {

    private final String mainClass;
    private final Collection<String> packagePrefixes;
    private final URL[] urls;
    private final Logger logger = LoggerHelpers.logger();
    private Thread appThread;
    private ClassLoader classLoader;

    public ApplicationLoader(String mainClass, Collection<String> packagePrefixes, URL[] urls) {
        this.mainClass = mainClass;
        this.packagePrefixes = packagePrefixes;
        this.urls = urls;
    }

    public synchronized void start() {
        Thread.currentThread().setName("zackbumm");

        logger.info("Starting Application " + mainClass);

        ClassLoader parentClassLoader = getClass().getClassLoader();
        classLoader = new SubpackageClassLoader(
                urls,
                parentClassLoader,
                packagePrefixes
        );

        // Start the application in a new thread
        appThread = new Thread(() -> {
            Thread.currentThread().setName("main");
            try {
                Class<?> clazz = classLoader.loadClass(mainClass);
                Method mainMethod = clazz.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) new String[]{});
            } catch (Exception e) {
                if (e.getCause() instanceof InterruptedException) {
                    logger.fine("App Thread was interrupted");
                } else {
                    logger.severe("Failed to invoke main method on " + mainClass + ".");
                    stop();
                    throw new RuntimeException(e);
                }
            }
        });
        appThread.start();
    }

    public synchronized void stop() {
        logger.info("Stopping Application " + mainClass);
        if (appThread != null) {
            logger.info("Interrupting existing application thread");
            appThread.interrupt();
            try {
                appThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            appThread = null;
        }

        logger.fine("Clean up previous class loader and application instance");

        if (classLoader instanceof Closeable) {
            try {
                ((Closeable) classLoader).close();
            } catch (Exception e) {
                logger.warning("Failed to close class loader: " + e.getMessage());
            }
        }
        classLoader = null;
        System.gc();
    }

    public synchronized void restart() {
        logger.info("Restarting Application " + mainClass);
        stop();
        start();
    }
}
