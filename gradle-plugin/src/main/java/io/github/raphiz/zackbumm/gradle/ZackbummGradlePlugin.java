package io.github.raphiz.zackbumm.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZackbummGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        ZackbummExtension extension = target.getExtensions().create("zackbumm", ZackbummExtension.class);
        extension.getTaskName().convention("run");
        extension.getClassDirectories().convention(target.provider(() -> getTask(target, extension).getClasspath().filter((file) -> !file.isDirectory())));

        target.afterEvaluate((project) -> {
            JavaExec task = getTask(project, extension);
            List<String> packagePrefixes = extension.getPackagePrefixes().get();
            Duration debounceDuration = extension.getDebounceDuration().getOrNull();
            Duration shutdownPollingInterval = extension.getShutdownPollingInterval().getOrNull();
            String classDirectories = extension.getClassDirectories().get().getFiles()
                    .stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator));

            // Add zackbumm library jar to the runtime classpath
            task.setClasspath(task.getClasspath().plus(project.files(zackBummLibraryJar())));

            // Additional parameters to instruct the zackbumm devmode
            Map<String, String> configuration = new HashMap<>();
            configuration.put("zackbumm.mainClass", task.getMainClass().get());
            configuration.put("zackbumm.classDirectories", classDirectories);
            configuration.put("zackbumm.packagePrefixes", String.join(",", packagePrefixes));
            if (debounceDuration != null) {
                configuration.put("zackbumm.debounceDuration", debounceDuration.toMillis() + "");
            }
            if (shutdownPollingInterval != null) {
                configuration.put("zackbumm.shutdownPollingInterval", shutdownPollingInterval.toMillis() + "");
            }
            task.systemProperties(configuration);

            // Override the main class
            task.getMainClass().set("io.github.raphiz.zackbumm.DevMode");
        });
    }

    private static @NotNull JavaExec getTask(@NotNull Project target, ZackbummExtension extension) {
        return target.getTasks().named(extension.getTaskName().get(), JavaExec.class).get();
    }

    private File zackBummLibraryJar() {
        try {
            String className = "io.github.raphiz.zackbumm.DevMode";
            Class<?> clazz = Class.forName(className);

            String classResource = className.replace('.', File.separatorChar) + ".class";
            URL resource = clazz.getClassLoader().getResource(classResource);

            if (resource != null) {
                String url = resource.toString();
                if (url.startsWith("jar:file:")) {
                    String jarPath = url.substring("jar:file:".length(), url.indexOf("!"));
                    return new File(jarPath);
                } else {
                    throw new RuntimeException("Unknown resource location format: " + url);
                }
            } else {
                throw new RuntimeException("Class resource not found.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
