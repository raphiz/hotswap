package io.github.raphiz.zackbumm;

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


    // Take the urls from the current classloader - this will point to build/classes/java/test
    private final URL[] classLoaderUrls = new URL[]{getClass().getClassLoader().getResource("")};

    private final MockParentClassLoader parentClassLoader = new MockParentClassLoader();
    private final SubpackageClassLoader subpackageClassLoader = new SubpackageClassLoader(classLoaderUrls, parentClassLoader, Arrays.asList("something.else", SUBPACKAGE_PREFIX));

    @Test
    void loadClassWithMatchingPrefix() throws Exception {
        Class<?> clazz = subpackageClassLoader.loadClass(CLASS_FROM_SUBPACKAGE, false);

        assertNotNull(clazz);
        assertEquals(CLASS_FROM_SUBPACKAGE, clazz.getName());
        assertEquals(List.of("java.lang.Object"), parentClassLoader.loadedClasses);
    }

    @Test
    void loadClassWithoutMatchingPrefixDelegatesToParent() throws Exception {
        Class<?> clazz = subpackageClassLoader.loadClass(CLASS_FROM_OTHER_PACKAGE);

        assertNotNull(clazz);
        assertEquals(List.of("com.otherpackage.OtherClass"), parentClassLoader.loadedClasses);
    }

    @Test
    void loadAlreadyLoadedClass() throws Exception {
        Class<?> firstLoad = subpackageClassLoader.loadClass(CLASS_FROM_SUBPACKAGE, false);

        Class<?> secondLoad = subpackageClassLoader.loadClass(CLASS_FROM_SUBPACKAGE, false);

        assertSame(firstLoad, secondLoad);
        assertEquals(List.of("java.lang.Object"), parentClassLoader.loadedClasses);
    }

    @Test
    void loadNonExistentClassThrowsClassNotFoundException() {
        assertThrows(ClassNotFoundException.class, () -> subpackageClassLoader.loadClass("com.example.NonExistentClass", false));
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
