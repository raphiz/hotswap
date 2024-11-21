package io.github.raphiz.zackboom.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class ZackbummGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        System.out.println("Hello from the Gradle Plugin");
    }
}
