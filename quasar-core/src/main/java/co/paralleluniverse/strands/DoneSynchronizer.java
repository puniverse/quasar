/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class DoneSynchronizer implements Condition {
    public static final DoneSynchronizer instance = new DoneSynchronizer();
    
    private DoneSynchronizer() {
    }
    @Override
    public void register() {
        Strand.unpark(Strand.currentStrand());
    }

    @Override
    public void unregister() {
    }

    @Override
    public void await() throws InterruptedException, SuspendExecution {
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        return true;
    }

    @Override
    public void signal() {
    }

    @Override
    public void signalAll() {
    }
    
}
