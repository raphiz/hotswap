package io.github.raphiz.hotswap.gradle;

import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.time.Duration;

public interface HotswapExtension {
    Property<String> getTaskName();

    Property<FileCollection> getClassPath();

    Property<Duration> getDebounceDuration();

    Property<Duration> getShutdownPollingInterval();

    ListProperty<String> getPackagePrefixes();
}
