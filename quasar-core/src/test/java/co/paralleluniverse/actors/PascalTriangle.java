/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.DelayedVal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author eitan
 */
public class PascalTriangle {
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
            for (int j = 1; j < nbi.length-1; j++) {
                nbi[j] = bi[j].add(bi[j - 1]);
            }
            nbi[nbi.length-1] = BigInteger.ONE;
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
        DelayedVal<BigInteger> res = new DelayedVal<>();
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
        final DelayedVal<BigInteger> result;
        int level;
        int pos;
        BigInteger val;
        boolean isRight;
        ActorRef<PascalNodeMessage> left;
        int maxLevel;
        DelayedVal<BigInteger> leftResult = new DelayedVal<>();
        DelayedVal<BigInteger> rightResult = new DelayedVal<>();

        public PascalNode(DelayedVal<BigInteger> result, int level, BigInteger val, boolean isRight, ActorRef<PascalNodeMessage> left, int maxLevel) {
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
