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
package co.paralleluniverse.continuation;

import co.paralleluniverse.fibers.Callable;
import co.paralleluniverse.fibers.Suspend;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.ValuedContinuation;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author pron
 */
public class CoIterable<E> implements Iterable<E> {
    public static <E> CoIterable<E> iterable(Generator<E> generator) {
        return new CoIterable<E>(generator);
    }

    private final Generator<E> generator;

    public CoIterable(Generator<E> generator) {
        this.generator = generator;
    }

    @Override
    public Iterator<E> iterator() {
        return new CoIterator<>(generator);
    }

    private static class CoIterator<E> implements Iterator<E> {
        private final ValuedContinuation<CoIteratorScope, Void, E, Void> c;

        private boolean hasNextCalled;
        private boolean hasNext;
        private E next;

        public CoIterator(final Generator<E> generator) {
            c = new ValuedContinuation<CoIteratorScope, Void, E, Void>(CoIteratorScope.class, new Callable<Void>() {
                @Override
                @Suspendable
                public Void call() {
                    try {
                        generator.run();
                        return null;
                    } catch (SuspendExecution e) {
                        throw new AssertionError(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        @Override
        @Suspendable // nested
        public boolean hasNext() {
            if (!hasNextCalled) {
                try {
                    getNext();
                    hasNext = true;
                } catch (NoSuchElementException e) {
                    hasNext = false;
                }
                hasNextCalled = true;
            }
            return hasNext;
        }

        @Override
        @Suspendable // nested
        public E next() {
            if (hasNextCalled) {
                if (!hasNext)
                    throw new NoSuchElementException();
            } else
                getNext();

            hasNextCalled = false;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Suspendable // nested
        private E getNext() {
            // c = (ValuedContinuation<CoIteratorScope, Void, E, Void>) c.go();
            c.run();
            if (c.isDone()) {
                // System.err.println("PPPPP: DONE");
                throw new NoSuchElementException();
            }
            next = c.getPauseValue();
            // System.err.println("PPPPP: " + next);
            return next;
        }
    }

    @Suspendable
    public static <E> void produce(E element) throws CoIteratorScope {
        ValuedContinuation.pause(SCOPE, element);
    }

    // Unfortunately, throwables can't be generic. We would have wanted to CoIteratorScope<E>
    public static class CoIteratorScope extends Suspend {
    }

    private static final CoIteratorScope SCOPE = new CoIteratorScope();

    public static interface Generator<E> {
        void run() throws CoIteratorScope, SuspendExecution, InterruptedException; // Unfortunately, throwables can't be generic. We would have wanted to CoIteratorScope<E>
    }
}
