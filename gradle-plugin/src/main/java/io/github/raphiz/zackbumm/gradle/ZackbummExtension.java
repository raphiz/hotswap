package io.github.raphiz.zackbumm.gradle;

import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;

import java.time.Duration;

public interface ZackbummExtension {
    Property<String> getTaskName();

    Property<FileCollection> getClassDirectories();

    Property<Duration> getDebounceDuration();

    Property<Duration> getShutdownPollingInterval();
}
