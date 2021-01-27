package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.test.*;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.*;
import org.junit.*;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import static co.paralleluniverse.fibers.TestsHelper.*;
import static org.junit.Assert.*;

public class ClassLoaderTest {
    /**
     * Test instrumentation of @Suspendable classes loaded dynamically in a custom classloader
     */
    @Test
    public void testSuspendableMethodsLoadedDynamically() {
        final ArrayList<String> results = new ArrayList<>();
        try {
            try {
                URI currentURL = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                System.out.println(currentURL.toString());
                URL testClassesURL = currentURL.resolve("../classloadertest/").toURL();
                System.out.println(testClassesURL.toString());
                ClassLoader cl = new URLClassLoader(new URL[]{testClassesURL});
                Class<?> testClass = cl.loadClass("co.paralleluniverse.fibers.dynamic.DynamicallyLoadedSuspendable");
                Constructor<?> constructor = testClass.getConstructor();
                final TestInterface testInstance = (TestInterface) constructor.newInstance();

                assertEquals(cl, testInstance.getClass().getClassLoader());
                assertEquals(ClassLoader.getSystemClassLoader(), TestInterface.class.getClassLoader());

                Fiber co = new Fiber((String) null, null, (SuspendableCallable) null) {
                    @Override
                    protected Object run() throws SuspendExecution, InterruptedException {
                        testInstance.test(results);
                        return null;
                    }
                };
                for (int i = 0; i < 6; i++) {
                    exec(co);
                }
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        } finally {
            System.out.println(results);
        }

        assertEquals(17, results.size());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "d1", "d2", "b1", "b2", "f", "o1", "d1", "d2", "b1", "b2", "o2", "b1"), results);
    }

    /**
     * Test instrumentation of @Suspendable class loaded twice in distinct classloaders to ensure it is instrumented properly each time
     */
    @Test
    public void testSuspendableClassLoadedTwice() {
        final ArrayList<String> results1 = new ArrayList<>();
        final ArrayList<String> results2 = new ArrayList<>();
        try {
            try {
                URI currentURL = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                System.out.println(currentURL.toString());
                URL testClassesURL = currentURL.resolve("../classloadertest/").toURL();
                System.out.println(testClassesURL.toString());
                ClassLoader cl1 = new URLClassLoader(new URL[]{testClassesURL});
                ClassLoader cl2 = new URLClassLoader(new URL[]{testClassesURL});
                Class<?> testClass1 = cl1.loadClass("co.paralleluniverse.fibers.dynamic.DynamicallyLoadedSuspendable");
                Class<?> testClass2 = cl2.loadClass("co.paralleluniverse.fibers.dynamic.DynamicallyLoadedSuspendable");
                Constructor<?> constructor1 = testClass1.getConstructor();
                Constructor<?> constructor2 = testClass2.getConstructor();
                final TestInterface testInstance1 = (TestInterface) constructor1.newInstance();
                final TestInterface testInstance2 = (TestInterface) constructor2.newInstance();

                assertEquals(cl1, testInstance1.getClass().getClassLoader());
                assertEquals(cl2, testInstance2.getClass().getClassLoader());
                assertEquals(ClassLoader.getSystemClassLoader(), TestInterface.class.getClassLoader());

                Fiber co1 = new Fiber((String) null, null, (SuspendableCallable) null) {
                    @Override
                    protected Object run() throws SuspendExecution, InterruptedException {
                        testInstance1.test(results1);
                        return null;
                    }
                };

                Fiber co2 = new Fiber((String) null, null, (SuspendableCallable) null) {
                    @Override
                    protected Object run() throws SuspendExecution, InterruptedException {
                        testInstance2.test(results2);
                        return null;
                    }
                };
                exec(co2);
                for (int i = 0; i < 6; i++) {
                    exec(co1);
                }
                exec(co2);
                exec(co2);
                exec(co2);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        } finally {
            System.out.println(results1);
            System.out.println(results2);
        }

        assertEquals(17, results1.size());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "d1", "d2", "b1", "b2", "f", "o1", "d1", "d2", "b1", "b2", "o2", "b1"), results1);
        assertEquals(12, results2.size());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "d1", "d2", "b1", "b2", "f", "o1", "d1"), results2);
    }

    /**
     * Test instrumentation of a fiber implementation class that is loaded dynamically.
     */
    @Test
    public void testDynamicallyLoadedFiber() {
        ArrayList<String> results = null;
        try {
            try {
                URI currentURL = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                System.out.println(currentURL.toString());
                URL testClassesURL = currentURL.resolve("../classloadertest/").toURL();
                System.out.println(testClassesURL.toString());
                ClassLoader cl = new URLClassLoader(new URL[]{testClassesURL});
                Class<?> testClass = cl.loadClass("co.paralleluniverse.fibers.dynamic.DynamicallyLoadedFiber");
                Constructor<?> constructor = testClass.getConstructor();
                final Fiber<ArrayList<String>> testInstance = (Fiber<ArrayList<String>>) constructor.newInstance();

                assertEquals(cl, testInstance.getClass().getClassLoader());
                assertEquals(ClassLoader.getSystemClassLoader(), TestInterface.class.getClassLoader());

                for (int i = 0; i < 4; i++) {
                    assertFalse(testInstance.isDone());
                    exec(testInstance);
                }
                assertTrue(testInstance.isDone());
                results = testInstance.get();
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        } finally {
            System.out.println(results);
        }

        assertEquals(8, results.size());
        assertEquals(Arrays.asList("a", "b", "o1", "o2", "base1", "base2", "o3", "c"), results);
    }

}
