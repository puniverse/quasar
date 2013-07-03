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

    @Test
    public void testPascalSum() throws InterruptedException, ExecutionException, TimeoutException {
        int maxLevel = 10;
        Long get = new Fiber<Long>(new PascalNode(1, 1, true, null, maxLevel)).start().get(10, TimeUnit.SECONDS);
        assertEquals(Math.pow(2, maxLevel - 1), get, 0);
    }

    class RightBrother {
        PascalNode node;

        public RightBrother(PascalNode pn) {
            this.node = pn;
        }
        
    }
    class Nepew {
        PascalNode node;        

        public Nepew(PascalNode pn) {
            this.node = pn;
        }
    }

    class PascalNode extends BasicActor<Object, Long> {
        int level;
        int pos;
        long val;
        boolean isRight;
        Actor<Object> left;
        int maxLevel;

        public PascalNode(int level, long val, boolean isRight, Actor<Object> left, int maxLevel) {
            this.level = level;
            this.val = val;
            this.isRight = isRight;
            this.left = left;
            this.maxLevel = maxLevel;
        }

        Fiber spawn() {
            return new Fiber<>(this).start();
        }

        @Override
        protected Long doRun() throws InterruptedException, SuspendExecution {
            if (level == maxLevel)
                return val;

            PascalNode leftChild;
            if (left != null) {
                left.send(new RightBrother(this));
                leftChild = receive(Nepew.class).node;
            } else {
                leftChild = new PascalNode(level + 1, val, false, null, maxLevel);
                leftChild.spawn();
            }
            final PascalNode rb = isRight? null: receive(RightBrother.class).node;
            final PascalNode rightChild = new PascalNode(level + 1, val + (rb==null?0:rb.val), isRight, leftChild, maxLevel);
            rightChild.spawn();
            if (rb != null)
                rb.send(new Nepew(rightChild));

            try {
                if (left == null)
                    return leftChild.get() + rightChild.get();
                else
                    return rightChild.get();
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
