/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.SuspendExecution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
        } catch (ExecutionException e) {
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
    public void testReceiveAfterSleep() throws Exception {
        Actor<Message, Integer> actor = new Actor<Message, Integer>(fjPool, mailboxSize) {
            int counter;

            @Override
            protected Integer run() throws SuspendExecution, InterruptedException {
                Message m1 = receive();
                Message m2 = receive();
                return m1.num + m2.num;
            }
        }.start();

        actor.send(new Message(25));
        Thread.sleep(200);
        actor.send(new Message(17));

        assertThat(actor.get(), is(42));
    }

    @Test
    public void testSelectiveReceive() throws Exception {
        Actor<ComplexMessage, List<Integer>> actor = new Actor<ComplexMessage, List<Integer>>(fjPool, mailboxSize) {
            int counter;

            @Override
            protected List<Integer> run() throws SuspendExecution, InterruptedException {
                final List<Integer> list = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    receive(new MessageProcessor<ComplexMessage>() {
                        public boolean process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                            switch (m.type) {
                                case FOO:
                                    list.add(1);
                                    receive(m, new MessageProcessor<ComplexMessage>() {
                                        public boolean process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                                            switch (m.type) {
                                                case BAZ:
                                                    list.add(3);
                                                    return true;
                                                default:
                                                    return false;
                                            }
                                        }
                                    });
                                    return true;
                                case BAR:
                                    list.add(2);
                                    return true;
                                case BAZ:
                                    fail();
                                default:
                                    return false;
                            }
                        }
                    });
                }
                return list;
            }
        }.start();

        actor.send(new ComplexMessage(ComplexMessage.Type.FOO, 1));
        actor.send(new ComplexMessage(ComplexMessage.Type.BAR, 2));
        actor.send(new ComplexMessage(ComplexMessage.Type.BAZ, 3));

        assertThat(actor.get(), equalTo(Arrays.asList(1, 3, 2)));
    }

    @Test
    public void whenSimpleReceiveAndTimeoutThenReturnNull() throws Exception {
        Actor<Message, Void> actor = new Actor<Message, Void>(fjPool, mailboxSize) {
            int counter;

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                Message m;
                m = receive(50, TimeUnit.MILLISECONDS);
                assertThat(m.num, is(1));
                m = receive(50, TimeUnit.MILLISECONDS);
                assertThat(m.num, is(2));
                m = receive(50, TimeUnit.MILLISECONDS);
                assertThat(m, is(nullValue()));
                
                return null;
            }
        }.start();

        actor.send(new Message(1));
        Thread.sleep(20);
        actor.send(new Message(2));
        Thread.sleep(100);
        actor.send(new Message(3));
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

    static class ComplexMessage {
        enum Type {
            FOO, BAR, BAZ, WAT
        };
        final Type type;
        final int num;

        public ComplexMessage(Type type, int num) {
            this.type = type;
            this.num = num;
        }
    }
}
