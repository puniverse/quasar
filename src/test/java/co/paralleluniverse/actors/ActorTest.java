/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.SuspendExecution;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
    public void testReceive() {
        Actor<Message, Void> manager = new Actor(fjPool, mailboxSize) {
            int counter;

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                for (;;) {
                    receive(new MessageProcessor<Message>() {
                        @Override
                        public boolean process(Message m) {
                            counter++;
                            return true;
                        }
                    });
                }
            }
        }.start();
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
