/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads;

import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendableRunnable;
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
        LightweightThread c = new LightweightThread(new MergeTest());
        c.exec();
    }
}
