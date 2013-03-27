/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.matthiasmann.continuations;

import java.io.FileNotFoundException;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.Test;

/**
 *
 * @author mam
 */
public class MergeTest extends TestCase implements CoroutineProto {

    public static void throwsIO() throws IOException {
    }

    public void coExecute() throws SuspendExecution {
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
        Coroutine c = new Coroutine(new MergeTest());
        c.run();
    }
}
