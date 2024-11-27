package io.github.raphiz.zackboom.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.util.stream.Collectors;

public class ZackbummGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        target.afterEvaluate((project) -> {
            // TODO: Make name configurable via extension
            // TODO: Add more config options such as timeouts and additional directories to watch
            TaskProvider<JavaExec> taskProvider = project.getTasks().named("run", JavaExec.class);
            JavaExec task = taskProvider.get();
            String classesOutputs = taskProvider.get().getClasspath().getFiles().stream()
                    .map(File::getAbsolutePath)
                    .filter((file) -> !file.endsWith(".jar"))
                    .collect(Collectors.joining(File.pathSeparator));

            // Add zackbumm library jar to the runtime classpath
            task.setClasspath(task.getClasspath().plus(project.files(zackBummLibraryJar())));

            // Additional parameters to instruct the zackbumm devmode
            task.jvmArgs(
                    "-Dzackbumm.mainClass=" + task.getMainClass().get(),
                    "-Dzackbumm.packagePrefixes=com.example",
                    "-Dzackbumm.classesOutputs=" + classesOutputs
            );

            // Override the main class
            task.getMainClass().set("io.github.raphiz.zackbumm.DevMode");
        });
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
