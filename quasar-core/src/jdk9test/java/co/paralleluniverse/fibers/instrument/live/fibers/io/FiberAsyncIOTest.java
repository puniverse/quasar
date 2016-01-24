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
package co.paralleluniverse.fibers.instrument.live.fibers.io;

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.fibers.io.FiberFileChannel;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author pron
 */
//@Ignore
public final class FiberAsyncIOTest extends LiveInstrumentationTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;
    
    private static final int PORT = 1234;
    private static final Charset charset = Charset.forName("UTF-8");
    private static final CharsetEncoder encoder = charset.newEncoder();
    private static final CharsetDecoder decoder = charset.newDecoder();

    private final FiberScheduler scheduler;

    public FiberAsyncIOTest() {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
    }

    @Test
    @Suspendable
    public final void testFiberAsyncSocket() throws Exception {
        final IntChannel sync = Channels.newIntChannel(0);
        
        final Fiber server = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try (final FiberServerSocketChannel socket =
                     FiberServerSocketChannel.open().bind(new InetSocketAddress(PORT))) {
                sync.send(0); // Start client

                try (final FiberSocketChannel ch = socket.accept()) {

                    final ByteBuffer buf = ByteBuffer.allocateDirect(1024);

                    // long-typed reqeust/response
                    int n = ch.read(buf);

                    assertThat(n, is(8)); // we assume the message is sent in a single packet

                    buf.flip();
                    final long req = buf.getLong();

                    assertThat(req, is(12345678L));

                    buf.clear();
                    final long res = 87654321L;
                    buf.putLong(res);
                    buf.flip();

                    n = ch.write(buf);

                    assertThat(n, is(8));

                    // String reqeust/response
                    buf.clear();
                    ch.read(buf); // we assume the message is sent in a single packet

                    buf.flip();
                    final String req2 = decoder.decode(buf).toString();

                    assertThat(req2, is("my request"));

                    final String res2 = "my response";
                    ch.write(encoder.encode(CharBuffer.wrap(res2)));
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        final Fiber client = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                sync.receive(); // Wait that the server is ready
            } catch (InterruptedException ex) {
                // This should never happen
                throw new AssertionError(ex);
            }

            try (final FiberSocketChannel ch = FiberSocketChannel.open(new InetSocketAddress(PORT))) {
                final ByteBuffer buf = ByteBuffer.allocateDirect(1024);

                // long-typed reqeust/response
                final long req = 12345678L;
                buf.putLong(req);
                buf.flip();

                int n = ch.write(buf);

                assertThat(n, is(8));

                buf.clear();
                n = ch.read(buf);

                assertThat(n, is(8)); // we assume the message is sent in a single packet

                buf.flip();
                final long res = buf.getLong();

                assertThat(res, is(87654321L));

                // String reqeust/response
                final String req2 = "my request";
                ch.write(encoder.encode(CharBuffer.wrap(req2)));

                buf.clear();
                ch.read(buf); // we assume the message is sent in a single packet

                buf.flip();
                final String res2 = decoder.decode(buf).toString();

                assertThat(res2, is("my response"));

                // verify that the server has closed the socket
                buf.clear();
                n = ch.read(buf);

                assertThat(n, is(-1));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        client.join();
        server.join();

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(2L));
    }

    @Test
    @Suspendable
    public final void testFiberAsyncFile() throws Exception {
        new Fiber(scheduler, (SuspendableRunnable) () -> {
            try (final FiberFileChannel ch = FiberFileChannel.open(Paths.get(System.getProperty("user.home"), "fibertest.bin"), READ, WRITE, CREATE, TRUNCATE_EXISTING)) {
                final ByteBuffer buf = ByteBuffer.allocateDirect(1024);

                final String text = "this is my text blahblah";
                ch.write(encoder.encode(CharBuffer.wrap(text)));

                ch.position(0);
                ch.read(buf);

                buf.flip();
                String read = decoder.decode(buf).toString();

                assertThat(read, equalTo(text));

                buf.clear();

                ch.position(5);
                ch.read(buf);

                buf.flip();
                read = decoder.decode(buf).toString();

                assertThat(read, equalTo(text.substring(5)));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

        }).start().join();

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(1L));
    }
}
