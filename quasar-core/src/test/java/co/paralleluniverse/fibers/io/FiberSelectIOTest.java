/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author pron
 */
public class FiberSelectIOTest {
    private static final int PORT = 1234;
    private static final Charset charset = Charset.forName("UTF-8");
    private static final CharsetEncoder encoder = charset.newEncoder();
    private static final CharsetDecoder decoder = charset.newDecoder();
    private final FiberScheduler scheduler;

    public FiberSelectIOTest() {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFiberSelectSocket() throws Exception {
        final IntChannel sync = Channels.newIntChannel(0);
        
        final Fiber server = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try (FiberServerSocketChannel socket = FiberServerSocketChannel.open().bind(new InetSocketAddress(PORT))) {
                    sync.send(0); // Start client

                    try (FiberSocketChannel ch = socket.accept()) {

                        ByteBuffer buf = ByteBuffer.allocateDirect(1024);

                        // long-typed reqeust/response
                        int n = ch.read(buf);

                        assertThat(n, is(8)); // we assume the message is sent in a single packet

                        buf.flip();
                        long req = buf.getLong();

                        assertThat(req, is(12345678L));

                        buf.clear();
                        long res = 87654321L;
                        buf.putLong(res);
                        buf.flip();

                        n = ch.write(buf);

                        assertThat(n, is(8));

                        // String reqeust/response
                        buf.clear();
                        ch.read(buf); // we assume the message is sent in a single packet

                        buf.flip();
                        String req2 = decoder.decode(buf).toString();

                        assertThat(req2, is("my request"));

                        String res2 = "my response";
                        ch.write(encoder.encode(CharBuffer.wrap(res2)));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        final Fiber client = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    sync.receive(); // Wait that the server is ready
                } catch (InterruptedException ex) {
                    // This should never happen
                    throw new AssertionError(ex);
                }

                try (FiberSelectSocketChannel ch = FiberSelectSocketChannel.open(new InetSocketAddress(PORT))) {
                    ByteBuffer buf = ByteBuffer.allocateDirect(1024);

                    // long-typed reqeust/response
                    long req = 12345678L;
                    buf.putLong(req);
                    buf.flip();

                    int n = ch.write(buf);

                    assertThat(n, is(8));

                    buf.clear();
                    n = ch.read(buf);

                    assertThat(n, is(8)); // we assume the message is sent in a single packet

                    buf.flip();
                    long res = buf.getLong();

                    assertThat(res, is(87654321L));

                    // String reqeust/response
                    String req2 = "my request";
                    ch.write(encoder.encode(CharBuffer.wrap(req2)));

                    buf.clear();
                    ch.read(buf); // we assume the message is sent in a single packet

                    buf.flip();
                    String res2 = decoder.decode(buf).toString();

                    assertThat(res2, is("my response"));

                    // verify that the server has closed the socket
                    buf.clear();
                    n = ch.read(buf);

                    assertThat(n, is(-1));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        client.join();
        server.join();
    }
}
