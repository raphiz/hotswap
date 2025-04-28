package io.github.raphiz.hotswap;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.github.raphiz.hotswap.DevMode.Configuration.parse;
import static org.junit.jupiter.api.Assertions.*;

class DevModeConfigTest {
    @Test
    void failsWhenNoMainClassIsProvided() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            parse(Map.of(), validArgs());
        });
        assertEquals("Main class must be provided", exception.getMessage());
    }

    @Test
    void setsMainClassProperly() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.mainClass", "com.example.Foo");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals("com.example.Foo", configuration.mainClass);
    }

    @Test
    void setsSetOfPackagePrefixes() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.packagePrefixes", "com.example,io.github.raphiz,foo.bar");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(Set.of("com.example", "io.github.raphiz", "foo.bar"), configuration.packagePrefixes);
    }

    @Test
    void acceptsMissingEmptyPackagePrefixes() {
        Map<String, String> properties = validConfigurationProperties();
        properties.remove("hotswap.packagePrefixes");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertNull(configuration.packagePrefixes);
    }

    @Test
    void setsClassPathBasedOfProperties() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.classPath", "/path/to/foo" + File.pathSeparator + "/path/to/bar" + File.pathSeparator + "relative/path");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(Set.of(Path.of("/path/to/foo"), Path.of("/path/to/bar"), Path.of("relative/path")), configuration.classPath);
    }

    @Test
    void usesEntireClassPathIfClassesPathIsNotSet() {
        Map<String, String> properties = validConfigurationProperties();
        properties.remove("hotswap.classPath");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertFalse(configuration.classPath.isEmpty());
        assertTrue(configuration.classPath.stream().anyMatch(Files::isDirectory));
        assertTrue(configuration.classPath.stream().allMatch(Files::exists));
    }

    @Test
    void setsShutdownPollingInterval() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.shutdownPollingInterval", Duration.ofSeconds(42).toMillis() + "");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(configuration.shutdownPollingInterval, Duration.ofSeconds(42));
    }

    @Test
    void shutdownPollingIntervalFailsForNonNumericValue() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.shutdownPollingInterval", "abc");

        assertThrows(IllegalArgumentException.class, () -> parse(properties, validArgs()));
    }

    @Test
    void setsDefaultShutdownPollingIntervalForEmptyString() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.shutdownPollingInterval", "");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(configuration.shutdownPollingInterval, Duration.ofSeconds(5));
    }

    @Test
    void setsDefaultShutdownPollingIntervalIfNotProvided() {
        Map<String, String> properties = validConfigurationProperties();
        properties.remove("hotswap.shutdownPollingInterval");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(configuration.shutdownPollingInterval, Duration.ofSeconds(5));
    }


    @Test
    void setsDebounceDurationInterval() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.debounceDuration", Duration.ofSeconds(42).toMillis() + "");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(configuration.debounceDuration, Duration.ofSeconds(42));
    }

    @Test
    void debounceDurationIntervalForNonNumericValue() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.debounceDuration", "abc");

        assertThrows(IllegalArgumentException.class, () -> parse(properties, validArgs()));
    }

    @Test
    void setsDefaultForDebounceDurationIntervalForEmptyString() {
        Map<String, String> properties = validConfigurationProperties();
        properties.put("hotswap.debounceDuration", "");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(configuration.debounceDuration, Duration.ofMillis(100));
    }

    @Test
    void setsDefaultDebounceDurationIntervalIfNotProvided() {
        Map<String, String> properties = validConfigurationProperties();
        properties.remove("hotswap.debounceDuration");

        DevMode.Configuration configuration = parse(properties, validArgs());

        assertEquals(configuration.debounceDuration, Duration.ofMillis(100));
    }

    @Test
    void setsArgsWithProvidedArgs() {
        Map<String, String> properties = validConfigurationProperties();
        String[] args = new String[]{"A", "B"};

        DevMode.Configuration configuration = parse(properties, args);

        assertEquals(configuration.args, args);
    }

    private static Map<String, String> validConfigurationProperties() {
        return new HashMap<>(Map.of(
                "hotswap.mainClass", "com.example.MainClass",
                "hotswap.classPath", "build/classes",
                "hotswap.packagePrefixes", "com.example.foo.bar"
        )
        );
    }

    private static String[] validArgs() {
        return new String[]{};
    }
}