package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.suspend.Instrumented;
import org.objectweb.asm.Type;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test to check NPE in MethodDatabase.ClassEntry.equals.
 * See https://github.com/puniverse/quasar/issues/286
 * Duplicate DB entries are actually caused by a threading issue and a race in the instrumentation.
 * This leads to duplicate class name in the database but with different ClassEntry objects.
 * When testing ClassEntry objects for equality the equals operator is blowing up as the superName can be null.
 * @author Paul Hatcher
 */
@Instrumented
public class MethodDatabaseDuplicate {

    @Test
    public void testSuspend() throws IOException {
        final String className = Type.getInternalName(MethodDatabaseDuplicate.class);
        final QuasarInstrumentor instrumentor = new QuasarInstrumentor(false);
        final MethodDatabase db = instrumentor.getMethodDatabase(MethodDatabaseDuplicate.class.getClassLoader());

        // Create a DB entry for this class with null super.
        db.getOrCreateClassEntry(className, null);

        // Parse this class, just to exercise parsing and DB.
        // Not strictly necessary for exception to occur. But good for coverage.
        try (final InputStream in = MethodDatabaseDuplicate.class.getResourceAsStream("MethodDatabaseDuplicate.class")) {
            instrumentor.instrumentClass(MethodDatabaseDuplicate.class.getClassLoader(), MethodDatabaseDuplicate.class.getName(), in, true);
        }

        // Now make it blow up by calling recordSuspendableMethods with a null super.
        // This will find the duplicate but with a different (new) class entry.
        db.recordSuspendableMethods(className, new MethodDatabase.ClassEntry(null));
    }
}
