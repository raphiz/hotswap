package io.github.raphiz.zackbumm;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public class SubpackageClassLoader extends URLClassLoader {
    private final Collection<String> packagePrefixes;

    public SubpackageClassLoader(URL[] urls, ClassLoader parent, Collection<String> packagePrefixes) {
        super(urls, parent);
        this.packagePrefixes = packagePrefixes;
    }

    @Override
    protected synchronized Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            if (classNameStartsWithPackagePrefix(className)) {

                Class<?> clazz = findLoadedClass(className);
                if (clazz == null) {
                    clazz = findClass(className);
                }

                if (resolve) {
                    resolveClass(clazz);
                }

                return clazz;
            } else {
                return super.loadClass(className, resolve);
            }
        }
    }

    private boolean classNameStartsWithPackagePrefix(String name) {
        return packagePrefixes.stream().anyMatch(name::startsWith);
    }
}