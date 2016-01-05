/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

import java.io.IOException;


/**
 *
 * @author mam
 */
public class MergeTest implements SuspendableRunnable {

    public static void throwsIO() throws IOException {
    }

    @Override
    public void run() {
        try {
            throwsIO();
        } catch(final IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMerge() {
        Fiber c = new Fiber<>((String)null, null, new MergeTest());
        TestsHelper.exec(c);
    }
}
