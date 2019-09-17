package co.paralleluniverse.fibers.instrument;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClassLoaderExclusionTest {
    private QuasarInstrumentor instrumentor;

    @Before
    public void setup() {
        instrumentor = new QuasarInstrumentor(false);
    }

    @Test
    public void testAcceptWhenNoExclusions() {
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingEverything() {
        instrumentor.addExcludedClassLoader("**");
        assertTrue(instrumentor.isExcludedClassLoader("X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingDefaultPackageOnly() {
        instrumentor.addExcludedClassLoader("*");
        assertTrue(instrumentor.isExcludedClassLoader("X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingFirstLevel() {
        instrumentor.addExcludedClassLoader("*.*");
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingFirstLevelAndBelow() {
        instrumentor.addExcludedClassLoader("*.**");
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingSecondLevel() {
        instrumentor.addExcludedClassLoader("*.*.*");
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingSecondLevelAndBelow() {
        instrumentor.addExcludedClassLoader("*.*.**");
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingThirdLevel() {
        instrumentor.addExcludedClassLoader("*.*.*.*");
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingThirdLevelAndBelow() {
        instrumentor.addExcludedClassLoader("*.*.*.**");
        assertFalse(instrumentor.isExcludedClassLoader("X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.d.X"));
    }

    @Test
    public void testRejectingSpecificLoader() {
        instrumentor.addExcludedClassLoader("**.LOADER");
        assertTrue(instrumentor.isExcludedClassLoader("a.LOADER"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.LOADER"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.LOADER"));

        assertFalse(instrumentor.isExcludedClassLoader("a.LOAD"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.LOAD"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.LOAD"));
    }

    @Test
    public void testRejectingBySingleCharcterGlobs() {
        instrumentor.addExcludedClassLoader("**.?X");
        assertTrue(instrumentor.isExcludedClassLoader("a.1X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.1X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.1X"));

        assertTrue(instrumentor.isExcludedClassLoader("a.2X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.2X"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.2X"));

        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }

    @Test
    public void testRejectingnamesWithDollars() {
        instrumentor.addExcludedClassLoader("**.X$Y");
        assertTrue(instrumentor.isExcludedClassLoader("a.X$Y"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.X$Y"));
        assertTrue(instrumentor.isExcludedClassLoader("a.b.c.X$Y"));

        assertFalse(instrumentor.isExcludedClassLoader("a.XY"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.XY"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.XY"));

        assertFalse(instrumentor.isExcludedClassLoader("a.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.X"));
        assertFalse(instrumentor.isExcludedClassLoader("a.b.c.X"));
    }
}
