# ♨️ Hotswap

**Hotswap** is a [Java library](https://search.maven.org/artifact/io.github.raphiz/hotswap-lib/) and [Gradle plugin](https://plugins.gradle.org/plugin/io.github.raphiz.hotswap) providing a lightweight and efficient **hot reloading experience**, similar to [Spring Boot Devtools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools.restart), for *any* JVM application during development.

> [!NOTE]
> This library is relatively new, and feedback or contributions are welcome!

## 📌 Main Benefits

- **Framework and language agnostic**: Works seamlessly with any JVM application, including Java or Kotlin, and frameworks such as Http4k or Javalin.
- **Faster iteration**: Accelerates the development feedback loop by removing the need for manual restarts.
- **Reduced startup time**: Eliminates the overhead of restarting the JVM and reloading unchanged classes.
- **Zero dependencies**: Integrates easily without external dependencies.
- **Non-intrusive**: Designed exclusively for development and does not impact production deployments.
- **Extendable**: Provides a simple, flexible core that can be customized to your project's specific requirements.
- **Browser LiveReload (coming soon)**: Supports automatic browser reloads for browser-connected applications.

## 🚀 How it Works

Hotswap continuously monitors your classpath for changes, which are triggered by [manual recompilation](#-trigger-restarts).
When changes occur, Hotswap gracefully [interrupts](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html) your running application, signaling it to shut down.
Your application must handle this interruption by properly closing resources and stopping running services.
Once the shutdown completes, Hotswap immediately restarts your application using updated classes.

This restart mechanism uses two class loaders, similar to [Spring Boot Devtools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools.restart).
A base class loader manages unchanged classes, such as those from the JDK and third-party libraries, while a restart class loader specifically handles classes currently under development.
On restart, the restart class loader is discarded and recreated.

If you encounter delays or inconsistencies during reloading, parameters like `debounceDuration` can be adjusted to better match your development environment.

## 🛠️ Usage

### As Gradle Plugin

Add the following to your `build.gradle.kts`:

```kotlin
plugins {
    application
    id("io.github.raphiz.hotswap")
}

application {
    mainClass.set("your.main.class")
}

hotswap {
    packagePrefixes.set(listOf("com.example")) // Monitor specific packages
}
```

### As Library

*Additional documentation will follow.*

## ⚙️ Configuration

### Gradle Plugin Options

| Option             | Type           | Description                                          | Required | Default          |
| ------------------ | -------------- | ---------------------------------------------------- | -------- | ---------------- |
| `packagePrefixes`  | `List<String>` | package names of the actively developed classes      | yes      | -                |
| `task`             | `String`       | Name of the gradle task used to run your application | no       | `run`            |
| `classDirectories` | `List<Path>`   | Directories to watch for changes                     | no       | Task's classpath |
| `debounceDuration` | `Duration`     | Aggregation period for rapid file changes            | no       | `100ms`          |

### Library Options

| Option                    | Type          | Description                                      | Required | Default |
| ------------------------- | ------------- | ------------------------------------------------ | -------- | ------- |
| `mainClass`               | `String`      | Your application's main class                    | yes      | -       |
| `packagePrefixes`         | `Set<String>` | Packages included for reload monitoring          | yes      | -       |
| `classDirectories`        | `Set<Path>`   | Directories watched for class updates            | yes      | -       |
| `shutdownPollingInterval` | `Duration`    | Interval before logging warnings during shutdown | no       | `5s`    |
| `debounceDuration`        | `Duration`    | Aggregation delay for rapid file changes         | no       | `100ms` |

## 📦 Trigger Restarts

Hotswap does not include its own compiler or build system.
Instead, it integrates seamlessly with your existing toolchain.
To trigger restarts, you simply recompile your code - Hotswap will detect any resulting class file changes and restart the application accordingly.

The most common ways to trigger recompilation include:

- **Using your IDE**: Compile changes manually using your IDE (e.g., IntelliJ IDEA via Ctrl/Cmd + F9), or [configure it to compile automatically on save](https://www.jetbrains.com/help/idea/compiling-applications.html#compile-on-save).
- **Using Gradle in continuous mode**: Run `gradle -t classes` or any other relevant task with the `-t` flag for [continuous building](https://docs.gradle.org/current/userguide/continuous_builds.html).

Hotswap listens for changes in the compiled output (i.e., class files), not the source files themselves.
As long as the build process updates class files in the directories Hotswap is watching, the reload will be triggered.

## 📝 Logging

Hotswap uses Java's built-in logging infrastructure (`java.util.logging`).
If your application uses a different logging framework (e.g., SLF4J, Logback), additional libraries or bridges might be necessary.

For detailed guidance on integrating logging frameworks, see [this article](https://stackify.com/logging-java/).

## 🔧 Development

### Prerequisites

Recommended: Use `nix` and `direnv` for managing dependencies and your development environment.

- Install [`nix`](https://nix.dev/install-nix#install-nix).
- Install [`direnv`](https://direnv.net/docs/installation.html).
- Execute `direnv allow` in your project directory.

Using Nix is recommended but not mandatory; it provides tooling, dependencies, and pre-commit configurations.

### Running Tests

```bash
./gradlew check
```

## 📄 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.
