/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author eitan
 */
public class PascalTriangle {
    public PascalTriangle() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //

    private class a {
        int a;
    }

    private class b {
        int b;
    }
    
    @Ignore
    @Test
    public void hello1() throws InterruptedException, ExecutionException, TimeoutException {
        List<Fiber> list = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            list.add(spawn(new BasicActor<Object, Void>() {
                @Override
                protected Void doRun() throws InterruptedException, SuspendExecution {
                    try {
                        final BasicActor<Object, Void> receiver = new BasicActor<Object, Void>() {
                            @Override
                            protected Void doRun() throws InterruptedException, SuspendExecution {
                                try {
                                    typedRecieve1(this, a.class);
                                    typedRecieve1(this, b.class);
                                } catch (TimeoutException ex) {
//                                    throw new RuntimeException(ex);
                                    System.out.println("TO");
                                    return null;
                                }
                                System.out.println("finished");
                                return null;
                            }
                        };
                        Fiber spawn = spawn(receiver);
                        receiver.send(new b());
//                        Fiber spawn1 = spawn(new BasicActor<Object, Void>() {
//                            @Override
//                            protected Void doRun() throws InterruptedException, SuspendExecution {
//                                reciever.send(new b());
//                                return null;
//                            }
//                        });

                        receiver.send(new a());
//                        Fiber spawn2 = spawn(new BasicActor<Object, Void>() {
//                            @Override
//                            protected Void doRun() throws InterruptedException, SuspendExecution {
//                                reciever.send(new a());
//                                System.out.println("sent "+this);
//                                return null;
//                            }
//                        });
//                        System.out.println("spawn");
//                        spawn2.join();
                        spawn.join();
//                        spawn1.join();
                        return null;
                    } catch (ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }));
        }
        for (Fiber fiber : list) {
            fiber.join();
        }
        Debug.dumpRecorder();
    }

    public Fiber spawn(final BasicActor<Object, Void> basicActor) {
        return new Fiber<>(basicActor).start();
    }

    <T> T typedRecieve1(final BasicActor actor, final Class<T> type) throws SuspendExecution, InterruptedException, TimeoutException {
        System.out.println("stat " + "begin" + " actor: " + actor);
        return type.cast(actor.receive(500, TimeUnit.MILLISECONDS, new MessageProcessor<Object>() {
            @Override
            public boolean process(Object m) throws SuspendExecution, InterruptedException {
                final boolean instance = type.isInstance(m);
                System.out.println("stat " + instance + " actor: " + actor + " m: " + m);
                return (instance);
            }
        }));
    }

    //@Test
    public void hello() throws InterruptedException, ExecutionException, TimeoutException {
        new Fiber<Void>(new PascalNode(1, 1, 1, true, null, 11)).start().join(10, TimeUnit.SECONDS);
    }

    class PascalResult {
        int res;
        PascalNode sender;

        public PascalResult(int res, PascalNode sender) {
            this.res = res;
            this.sender = sender;
        }
    }

    class PascalNode extends BasicActor<Object, Void> {
        int level;
        int pos;
        int n;
        boolean isRight;
        Actor<Object> left;
        int maxLevel;

        public PascalNode(int level, int pos, int n, boolean isRight, Actor<Object> left, int maxLevel) {
            this.level = level;
            this.pos = pos;
            this.n = n;
            this.isRight = isRight;
            this.left = left;
            this.maxLevel = maxLevel;
        }

        @Override
        public String toString() {
            return "PascalNode{" + "level=" + level + ", pos=" + pos + ", n=" + n + ", isRight=" + isRight + ", left=" + (left != null) + ", maxLevel=" + maxLevel + '}';
        }

        PascalResult getResFromRight() throws SuspendExecution, InterruptedException, TimeoutException {
            if (isRight)
                return new PascalResult(0, null);
            return typedRecieve(PascalResult.class);
        }

        <T> T typedRecieve(final Class<T> type) throws SuspendExecution, InterruptedException, TimeoutException {
            return type.cast(receive(500, TimeUnit.MILLISECONDS, new MessageProcessor<Object>() {
                @Override
                public boolean process(Object m) throws SuspendExecution, InterruptedException {
                    final boolean instance = type.isInstance(m);
                    if (!instance)
                        System.out.println("ni " + PascalNode.this);
                    return (instance);
                }
            }));
        }

        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            System.out.println("" + System.nanoTime() + " start " + this);
            if (level == maxLevel) {
                System.out.println("val is " + n);
                return null;
            }
            PascalNode leftChild;
            if (left != null) {
                left.send(new PascalResult(n, this));
                System.out.println("" + System.nanoTime() + " sent res from " + this + " to " + left);
                try {
                    leftChild = typedRecieve(PascalNode.class);
                } catch (TimeoutException ex) {
                    throw new RuntimeException("lc " + this, ex);
                }
            } else {
                leftChild = new PascalNode(level + 1, pos, n, false, null, maxLevel);
                new Fiber<Void>(leftChild).start();
            }
            final PascalResult resFromRight;
            try {
                resFromRight = getResFromRight();
            } catch (TimeoutException ex) {
                throw new RuntimeException("rv " + System.nanoTime() + this, ex);
            }
            final PascalNode rightChild = new PascalNode(level + 1, pos + 1, n + resFromRight.res, isRight, leftChild, maxLevel);
            new Fiber<Void>(rightChild).start();
            if (resFromRight.sender != null) {
                resFromRight.sender.send(rightChild);
            }
            try {
                leftChild.join();
                rightChild.join();
            } catch (ExecutionException ex) {
                Logger.getLogger(PascalTriangle.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }
}
