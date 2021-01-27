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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import static co.paralleluniverse.strands.channels.Selector.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author pron
 */
@RunWith(Parameterized.class)
public class GeneralSelectorTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;
    
    final boolean fiber;
    final int mailboxSize;
    final OverflowPolicy policy;
    final boolean singleConsumer;
    final boolean singleProducer;
    final FiberScheduler scheduler;

//    public GeneralSelectorTest() {
//        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
//        this.mailboxSize = 1;
//        this.policy = OverflowPolicy.BLOCK;
//        this.singleConsumer = false;
//        this.singleProducer = false;
//        this.fiber = false;
//
//        Debug.dumpAfter(15000, "channels.log");
//    }
    public GeneralSelectorTest(int mailboxSize, OverflowPolicy policy, boolean singleConsumer, boolean singleProducer) {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
        this.mailboxSize = mailboxSize;
        this.policy = policy;
        this.singleConsumer = singleConsumer;
        this.singleProducer = singleProducer;
        fiber = true;
    }

    @After
    public void tearDown() {
        scheduler.shutdown();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                    {5, OverflowPolicy.BLOCK, false, false},
                    {1, OverflowPolicy.BLOCK, false, false},
                    {-1, OverflowPolicy.THROW, true, false},
                    {5, OverflowPolicy.DISPLACE, true, false},
                    {5, OverflowPolicy.DROP, true, false},
                    {0, OverflowPolicy.BLOCK, false, false},});
    }

    private <Message> Channel<Message> newChannel() {
        return Channels.newChannel(mailboxSize, policy, singleProducer, singleConsumer);
    }

    void spawn(SuspendableRunnable r) {
        if (fiber)
            new Fiber(scheduler, r).start();
        else
            new Thread(Strand.toRunnable(r)).start();
    }

    <Message> Channel<Message>[] fanout(final ReceivePort<Message> in, final int n) {
        final Channel<Message>[] chans = new Channel[n];
        for (int i = 0; i < n; i++)
            chans[i] = newChannel();
        spawn(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (;;) {
                    Message m = in.receive();
                    // System.out.println("fanout: " + m);
                    if (m == null) {
                        for (Channel<Message> c : chans)
                            c.close();
                        break;
                    } else {
                        List<SelectAction<Message>> as = new ArrayList<>(n);
                        for (Channel<Message> c : chans)
                            as.add(send(c, m));
                        SelectAction<Message> sa = select(as);
                        // System.out.println("Wrote to " + sa.index());
                    }
                }
                //System.err.println("fanout done");
            }
        });
        return chans;
    }

    <Message> Channel<Message> fanin(final ReceivePort<Message>[] ins) {
        final Channel<Message> chan = newChannel();

        spawn(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (;;) {
                    List<SelectAction<Message>> as = new ArrayList<>(ins.length);
                    for (ReceivePort<Message> c : ins)
                        as.add(receive(c));
                    SelectAction<Message> sa = select(as);
                    // System.out.println("Received from " + sa.index());

                    Message m = sa.message();
                    // System.out.println("fanin: " + m);
                    if (m == null) {
                        chan.close();
                        break;
                    } else
                        chan.send(m);
                }
                //System.err.println("fanin done");
            }
        });
        return chan;
    }

    @Test
    public void testFans1() throws Exception {
        int nchans = 3;
        int n = 200;

        final Channel<Integer> out = newChannel();
        final Channel<Integer> in = fanin(fanout(out, nchans));

        for (int i = 0; i < n; i++) {
            //System.out.println("send: " + i);
            out.send(i);
            //System.out.println("receiving");
            Integer x = in.receive();
            //System.out.println("receied " + x);
            assertThat(x, is(i));
        }
        out.close();
        assertThat(in.receive(), nullValue());
        assertThat(in.isClosed(), is(true));
    }

    @Test
    public void testFans2() throws Exception {
        assumeThat(mailboxSize, is(1));
        int nchans = 10;
        int n = nchans;

        final Channel<Integer> out = newChannel();
        final Channel<Integer> in = fanin(fanout(out, nchans));

        for (int i = 0; i < n; i++) {
            out.send(i);
        }

        Thread.sleep(500);

        boolean[] ms = new boolean[n];
        for (int i = 0; i < n; i++) {
            Integer m = in.receive();
            ms[m] = true;
        }
        for (int i = 0; i < n; i++)
            assertThat(ms[i], is(true));


        out.close();
        assertThat(in.receive(), nullValue());
        assertThat(in.isClosed(), is(true));
    }
}
