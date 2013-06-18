/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.strands.SuspendableRunnable;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Matthias Mann
 */
public class ArrayTest implements SuspendableRunnable {

    private static final PatchLevel l1 = new PatchLevel();
    private static final PatchLevel[] l2 = new PatchLevel[] { l1 };
    private static final PatchLevel[][] l3 = new PatchLevel[][] { l2 };
    
    @Test
    public void testArray() {
        Fiber co = new Fiber((String)null, null, this);
        TestsHelper.exec(co);
        assertEquals(42, l1.i);
    }
    
    @Override
    public void run() throws SuspendExecution {
        PatchLevel[][] local_patch_levels = l3;
        PatchLevel patch_level = local_patch_levels[0][0];
        patch_level.setLevel(42);
    }
    
    public static class PatchLevel {
        int i;
    
        public void setLevel(int value) throws SuspendExecution {
            i = value;
        }
    }
}
