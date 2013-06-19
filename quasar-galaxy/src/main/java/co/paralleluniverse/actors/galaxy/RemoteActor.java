/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.galaxy;

import co.paralleluniverse.actors.LifecycleListener;
import co.paralleluniverse.strands.channels.Mailbox;

/**
 *
 * @author pron
 */
public class RemoteActor<Message> extends co.paralleluniverse.actors.RemoteActor<Message> {
    
    public RemoteActor(Object name, Mailbox<Object> mailbox, boolean backpressure) {
        super(name, mailbox, backpressure);
    }

    
    @Override
    protected void throwIn(RuntimeException e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeLifecycleListener(Object listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected LifecycleListener getLifecycleListener() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Throwable getDeathCause() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void interrupt() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
