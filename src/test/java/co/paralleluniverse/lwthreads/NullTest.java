/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.lwthreads;

import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendableRunnable;
import static org.junit.Assert.*;
import org.junit.Test;


/**
 *
 * @author Matthias Mann
 */
public class NullTest implements SuspendableRunnable {

    Object result = "b";
    
    @Test
    public void testNull() {
        LightweightThread co = new LightweightThread(this);
        int count = 1;
        while(!co.exec())
            count++;

        assertEquals(2, count);
        assertEquals("a", result);
    }
    
    @Override
    public void run() throws SuspendExecution {
        result = getProperty();
    }
    
    private Object getProperty() throws SuspendExecution {
        Object x = null;
        
        Object y = getProperty("a");
        if(y != null) {
            x = y;
        }
        
        return x;
    }

    private Object getProperty(String string) throws SuspendExecution {
        LightweightThread.park();
        return string;
    }

}
