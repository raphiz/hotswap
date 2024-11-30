package io.github.raphiz.zackbumm.gradle;

import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;

public interface ZackbummExtension {
    Property<String> getTaskName();
    Property<FileCollection> getClassDirectories();
}
