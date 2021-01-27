/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.dataflow.Val;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 *
 * @author eitan
 */
public class PascalTriangle {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;
    
    final MailboxConfig mailboxConfig = new MailboxConfig(4, Channels.OverflowPolicy.THROW);

    public PascalTriangle() {
    }

    @Ignore
    @Test
    public void testSeqPascalSum() {
        int maxLevel = 800;
        BigInteger[] bi = new BigInteger[1];
        bi[0] = BigInteger.ONE;
        BigInteger[] nbi = null;
        for (int i = 1; i < maxLevel; i++) {
            nbi = new BigInteger[bi.length + 1];
            nbi[0] = BigInteger.ONE;
            for (int j = 1; j < nbi.length - 1; j++) {
                nbi[j] = bi[j].add(bi[j - 1]);
            }
            nbi[nbi.length - 1] = BigInteger.ONE;
            bi = nbi;
        }
//        System.out.println(Arrays.toString(nbi));
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < nbi.length; i++) {
            sum = sum.add(nbi[i]);
        }
        assertEquals(BigInteger.valueOf(2).pow(maxLevel - 1), sum);
    }

    @Test
    public void testPascalSum() throws Exception {
        int maxLevel = 20; //800
        Val<BigInteger> res = new Val<>();
        new Fiber<Void>(new PascalNode(res, 1, BigInteger.ONE, true, null, maxLevel)).start();
        assertEquals(BigInteger.valueOf(2).pow(maxLevel - 1), res.get(10, TimeUnit.SECONDS));
    }

    static class PascalNodeMessage {

    }

    static class RightBrother extends PascalNodeMessage {
        final ActorRef<PascalNodeMessage> node;
        final BigInteger val;

        public RightBrother(ActorRef<PascalNodeMessage> pn, BigInteger result) {
            this.node = pn;
            this.val = result;
        }
    }

    static class Nephew extends PascalNodeMessage {
        final ActorRef<PascalNodeMessage> node;

        public Nephew(ActorRef<PascalNodeMessage> pn) {
            this.node = pn;
        }
    }

    class PascalNode extends BasicActor<PascalNodeMessage, Void> {
        final Val<BigInteger> result;
        int level;
        int pos;
        BigInteger val;
        boolean isRight;
        ActorRef<PascalNodeMessage> left;
        int maxLevel;
        Val<BigInteger> leftResult = new Val<>();
        Val<BigInteger> rightResult = new Val<>();

        public PascalNode(Val<BigInteger> result, int level, BigInteger val, boolean isRight, ActorRef<PascalNodeMessage> left, int maxLevel) {
            super(mailboxConfig);
            this.result = result;
            this.level = level;
            this.val = val;
            this.isRight = isRight;
            this.left = left;
            this.maxLevel = maxLevel;
        }

        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            if (level == maxLevel) {
                result.set(val);
                return null;
            }

            ActorRef<PascalNodeMessage> leftChild;
            if (left != null) {
                left.send(new RightBrother(ref(), val));
                leftChild = receive(Nephew.class).node;
            } else
                leftChild = new PascalNode(leftResult, level + 1, val, false, null, maxLevel).spawn();

            final RightBrother rb = isRight ? null : receive(RightBrother.class);
            final ActorRef<PascalNodeMessage> rightChild = new PascalNode(rightResult, level + 1, val.add(rb == null ? BigInteger.ZERO : rb.val), isRight, leftChild, maxLevel).spawn();
            if (rb != null)
                rb.node.send(new Nephew(rightChild));

            if (left == null)
                result.set(leftResult.get().add(rightResult.get()));
            else
                result.set(rightResult.get());
            return null;
        }
    }
}
