/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels.disruptor;

import co.paralleluniverse.fibers.SuspendExecution;
import com.lmax.disruptor.AlertException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public interface SequenceBarrier extends com.lmax.disruptor.SequenceBarrier {
    long waitFor1(long sequence) throws AlertException, InterruptedException, SuspendExecution;
    long waitFor1(long sequence, long timeout, TimeUnit unit) throws AlertException, InterruptedException, TimeoutException, SuspendExecution;
}
