package io.github.raphiz.hotswap;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubpackageClassLoaderTest {

    public static final String SUBPACKAGE_PREFIX = "com.example";
    public static final String CLASS_FROM_SUBPACKAGE = SUBPACKAGE_PREFIX + ".SomeClass";
    public static final String CLASS_FROM_OTHER_PACKAGE = "com.otherpackage.OtherClass";
    public static final String JDK_CLASS = "java.lang.String";
    public static final String NON_EXISTENT_CLASS = "com.example.NonExistentClass";

    // URLs pointing at build/classes/java/test (where SomeClass and OtherClass are compiled)
    private final URL[] classLoaderUrls = new URL[]{getClass().getClassLoader().getResource("")};

    private final MockParentClassLoader parentClassLoader = new MockParentClassLoader();
    private final SubpackageClassLoader loaderWithPackagePrefixes = new SubpackageClassLoader(
            classLoaderUrls,
            parentClassLoader,
            Arrays.asList("something.else", SUBPACKAGE_PREFIX)
    );
    private final SubpackageClassLoader loaderWithoutPackagePrefixes = new SubpackageClassLoader(
            classLoaderUrls,
            parentClassLoader,
            null
    );

    @Test
    void loadClassWithMatchingPrefix() throws Exception {
        Class<?> clazz = loaderWithPackagePrefixes.loadClass(CLASS_FROM_SUBPACKAGE, false);

        assertNotNull(clazz);
        assertEquals(CLASS_FROM_SUBPACKAGE, clazz.getName());
        assertEquals(List.of("java.lang.Object"), parentClassLoader.loadedClasses);
    }

    @Test
    void loadClassWithoutMatchingPrefixDelegatesToParent() throws Exception {
        Class<?> clazz = loaderWithPackagePrefixes.loadClass(CLASS_FROM_OTHER_PACKAGE);

        assertNotNull(clazz);
        assertEquals(CLASS_FROM_OTHER_PACKAGE, clazz.getName());
        assertEquals(List.of(CLASS_FROM_OTHER_PACKAGE), parentClassLoader.loadedClasses);
    }

    @Test
    void loadAlreadyLoadedClass() throws Exception {
        Class<?> first = loaderWithPackagePrefixes.loadClass(CLASS_FROM_SUBPACKAGE, false);
        Class<?> second = loaderWithPackagePrefixes.loadClass(CLASS_FROM_SUBPACKAGE, false);

        assertSame(first, second);
        assertEquals(List.of("java.lang.Object"), parentClassLoader.loadedClasses);
    }

    @Test
    void loadNonExistentClassThrows() {
        assertThrows(
                ClassNotFoundException.class,
                () -> loaderWithPackagePrefixes.loadClass(NON_EXISTENT_CLASS, false)
        );
    }

    @Test
    void loadJdkClassDelegatesToParentWhenNoPackagePrefixes() throws Exception {
        Class<?> clazz = loaderWithoutPackagePrefixes.loadClass(JDK_CLASS);

        assertNotNull(clazz);
        assertEquals(JDK_CLASS, clazz.getName());
        assertEquals(List.of(JDK_CLASS), parentClassLoader.loadedClasses);
    }

    @Test
    void loadNonJdkClassLoadsByChildWhenNoPackagePrefixes() throws Exception {
        Class<?> clazz = loaderWithoutPackagePrefixes.loadClass(CLASS_FROM_OTHER_PACKAGE, false);

        assertNotNull(clazz);
        assertEquals(CLASS_FROM_OTHER_PACKAGE, clazz.getName());
        // only superclass resolution got delegated
        assertEquals(List.of("java.lang.Object"), parentClassLoader.loadedClasses);
    }

    private class MockParentClassLoader extends URLClassLoader {
        final List<String> loadedClasses = new ArrayList<>();

        public MockParentClassLoader() {
            super(classLoaderUrls);
        }

        @Override
        protected synchronized Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
            loadedClasses.add(className);
            return getClass().getClassLoader().loadClass(className);
        }
    }
}
