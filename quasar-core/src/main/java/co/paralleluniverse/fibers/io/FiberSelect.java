/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author circlespainter
 */
public class FiberSelect {
    private static final Queue<Registration> registrationQ = new ConcurrentLinkedQueue<>();
    private static final Queue<SelectionKey> unregistrationQ = new ConcurrentLinkedQueue<>();

    private static Selector selector;
    private static Thread selectingThread;

    private static class Registration {
        final SelectableChannel sc;
        final int ops;
        final SelectorFiberAsync callback;

        Registration(final SelectableChannel sc, final int ops, final SelectorFiberAsync callback) {
            this.sc = sc;
            this.ops = ops;
            this.callback = callback;
        }
    }

    private static void init() throws IOException {
        if (selector == null) {
            selector = Selector.open();
            selectingThread =
                new Thread() {
                    @Override
                    public void run() {
                        while(true) {
                            try {
                                Registration r = registrationQ.poll();
                                while(r != null) {
                                    r.sc.register(selector, r.ops, r.callback);
                                    r = registrationQ.poll();
                                }
                                if (selector.select() > 0) {
                                    for(final SelectionKey k : selector.selectedKeys())
                                        ((SelectorFiberAsync) k.attachment()).complete(k, k.readyOps());
                                }
                                SelectionKey k = unregistrationQ.poll();
                                while(k != null) {
                                    k.cancel();
                                    k = unregistrationQ.poll();
                                }
                            } catch (final IOException ioe) {
                                shutdown();
                                throw new RuntimeException(ioe);
                            }
                        }
                    }
                };
            selectingThread.start();
        }
    }

    static void shutdown() {
        if (selector != null) {
            for(final SelectionKey k : selector.keys()) {
                k.cancel();
            }
            try (Selector s = selector) {
                selector = null;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    @Suspendable
    static void forRead(final SelectableChannel sc) throws IOException {
        init();
        final Selector s = selector;
        new SelectorFiberAsync(sc, s, SelectionKey.OP_READ).run();
    }

    @Suspendable
    static void forWrite(final SelectableChannel sc) throws IOException {
        init();
        final Selector s = selector;
        new SelectorFiberAsync(sc, s, SelectionKey.OP_WRITE).run();
    }

    @Suspendable
    static void forConnect(final SelectableChannel sc) throws IOException {
        init();
        final Selector s = selector;
        new SelectorFiberAsync(sc, s, SelectionKey.OP_CONNECT).run();
    }

    static void register(final SelectableChannel sc, final int ops, final SelectorFiberAsync callback) throws IOException {
        init();
        registrationQ.offer(new Registration(sc, ops, callback));
        selector.wakeup();
    }

    static void unregister(final SelectionKey key) throws IOException {
        init();
        unregistrationQ.offer(key);
        selector.wakeup();
    }
}
