/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;


/**
 *
 * @author mam
 */
public class MergeTest implements SuspendableRunnable {

    public static void throwsIO() throws IOException {
    }

    @Override
    public void run() throws SuspendExecution {
        try {
            throwsIO();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMerge() {
        Fiber c = new Fiber((String)null, null, new MergeTest());
        TestsHelper.exec(c);
    }
}
