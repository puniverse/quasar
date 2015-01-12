/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * {@link SendPort} that will discard all messages sent to it.
 *
 * @author circlespainter
 */
public class NullSendPort implements SendPort {

    @Override
    public void send(Object message) throws SuspendExecution, InterruptedException {
        // NOP
    }

    @Override
    public boolean send(Object message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return true; // NOP
    }

    @Override
    public boolean send(Object message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return true; // NOP
    }

    @Override
    public boolean trySend(Object message) {
        return true; // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public void close(Throwable t) {
        // NOP
    }
}
