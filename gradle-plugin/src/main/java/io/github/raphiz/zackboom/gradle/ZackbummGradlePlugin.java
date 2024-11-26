package io.github.raphiz.zackboom.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;

public class ZackbummGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project project) {
        project.afterEvaluate((project1) -> {
            JavaExec runTask = (JavaExec) project1.getTasks().named("run").get();
            // Add zackbumm library jar to the runtime classpath
            runTask.setClasspath(runTask.getClasspath().plus(project.files(zackBummLibraryJar())));

            // Additional parameters to instruct the zackbumm devmode
            runTask.jvmArgs(
                    "-Dzackbumm.mainClass=" + runTask.getMainClass().get(),
                    "-Dzackbumm.packagePrefixes=com.example",
                    // TODO: Add all source sets instead
                    "-Dzackbumm.classesOutputs=" + project.getLayout().getBuildDirectory().get().file("classes/java/main/")
            );

            // Override the main class
            runTask.getMainClass().set("io.github.raphiz.zackbumm.DevMode");
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
