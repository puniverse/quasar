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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author circlespainter
 */
public class FiberSelect {
    private static final Queue<Registration> registrationQ = new ConcurrentLinkedQueue<>();
    private static final Queue<SelectionKey> deregistrationQ = new ConcurrentLinkedQueue<>();

    private static final Selector selector;
    private static final Thread selectingThread;

    static {
        try {
            selector = Selector.open();
            selectingThread =
                new Thread() {
                    @Override
                    public void run() {
                        while(true) {
                            try {
                                // Add new registrations
                                processRegistrations();
                                // Select
                                selector.select();
                                // Process selection
                                processSelected();
                                // Process requested deregistrations
                                processDeregistrations();
                                // Clear deregistered and wakeups
                                selector.selectNow();
                            } catch (final Exception e) {
                                // TODO
                                e.printStackTrace();
                            }
                        }
                    }

                    private void processSelected() {
                        final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        SelectionKey key;
                        while (iterator.hasNext()) {
                            key = iterator.next();
                            iterator.remove();
                            if (key.isValid()) {
                                try {
                                    ((SelectorFiberAsync) key.attachment()).complete(key, key.readyOps());
                                } catch (final CancelledKeyException cke) { /* Just ignore the key */ }
                            }
                        }
                    }

                    private void processDeregistrations() throws ClosedChannelException {
                        SelectionKey k = deregistrationQ.poll();
                        while(k != null) {
                            k.cancel();
                            k = deregistrationQ.poll();
                        }
                    }

                    private void processRegistrations() throws ClosedChannelException {
                        Registration r = registrationQ.poll();
                        while(r != null) {
                            r.sc.register(selector, r.ops, r.callback);
                            r = registrationQ.poll();
                        }
                    }
                };
            selectingThread.start();
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
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

    static void shutdown() {
        if (selector != null) {
            for(final SelectionKey k : selector.keys()) {
                k.cancel();
            }
            try {
                selector.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    @Suspendable
    static void forRead(final SelectableChannel sc) throws IOException {
        final Selector s = selector;
        new SelectorFiberAsync(sc, s, SelectionKey.OP_READ).run();
    }

    @Suspendable
    static void forWrite(final SelectableChannel sc) throws IOException {
        final Selector s = selector;
        new SelectorFiberAsync(sc, s, SelectionKey.OP_WRITE).run();
    }

    @Suspendable
    static void forConnect(final SelectableChannel sc) throws IOException {
        final Selector s = selector;
        new SelectorFiberAsync(sc, s, SelectionKey.OP_CONNECT).run();
    }

    static void register(final SelectableChannel sc, final int ops, final SelectorFiberAsync callback) throws IOException {
        registrationQ.offer(new Registration(sc, ops, callback));
        selector.wakeup();
    }

    static void deregister(final SelectionKey key) throws IOException {
        deregistrationQ.offer(key);
        selector.wakeup();
    }
}
