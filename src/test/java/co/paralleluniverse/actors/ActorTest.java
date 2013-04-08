/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.SuspendExecution;
import java.util.concurrent.ExecutionException;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author pron
 */
public class ActorTest {
    static final int mailboxSize = 10;
    private ForkJoinPool fjPool;

    public ActorTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @Test
    public void whenActorThrowsExceptionThenGetThrowsIt() throws Exception {
        Actor<Message, Integer> actor = new Actor<Message, Integer>(fjPool, mailboxSize) {
            int counter;

            @Override
            protected Integer run() throws SuspendExecution, InterruptedException {
                throw new RuntimeException("foo");
            }
        }.start();
        
        try {
            actor.get();
            fail();
        } catch(ExecutionException e) {
            assertThat(e.getCause(), instanceOf(RuntimeException.class));
            assertThat(e.getCause().getMessage(), is("foo"));
        }
    }
    
    @Test
    public void whenActorReturnsValueThenGetReturnsIt() throws Exception {
        Actor<Message, Integer> actor = new Actor<Message, Integer>(fjPool, mailboxSize) {
            int counter;

            @Override
            protected Integer run() throws SuspendExecution, InterruptedException {
                return 42;
            }
        }.start();
        
        assertThat(actor.get(), is(42));
    }
    
    @Test
    public void testReceive() throws Exception {
        Actor<Message, Integer> actor = new Actor<Message, Integer>(fjPool, mailboxSize) {
            int counter;

            @Override
            protected Integer run() throws SuspendExecution, InterruptedException {
                Message m = receive();
                return m.num;
            }
        }.start();
        
        actor.send(new Message(15));
        
        assertThat(actor.get(), is(15));
    }

    @Test
    public void testSelectiveReceive() {
    }

    @Test
    public void testTimeout() {
    }

    @Test
    public void testLink() {
    }

    @Test
    public void testMonitor() {
    }

    static class Message {
        final int num;

        public Message(int num) {
            this.num = num;
        }
    }
}
