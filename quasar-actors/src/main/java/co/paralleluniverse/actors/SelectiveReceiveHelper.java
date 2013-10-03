/*
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class SelectiveReceiveHelper<Message> {
    private final Actor<Message, ?> actor;
    private Message currentMessage; // this works because channel is single-consumer

    public SelectiveReceiveHelper(Actor<Message, ?> actor) {
        if (actor == null)
            throw new NullPointerException("actor is null");
        this.actor = actor;
    }

    /**
     *
     * @param proc
     * @param timeout
     * @param unit
     * @throws TimeoutException
     * @throws LwtInterruptedException
     */
    public final <T> T receive(long timeout, TimeUnit unit, MessageProcessor<Message, T> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        assert Actor.currentActor() == null || Actor.currentActor() == actor;

        final Mailbox<Object> mailbox = actor.mailbox();

        actor.checkThrownIn();
        mailbox.maybeSetCurrentStrandAsOwner();

        final long start = timeout > 0 ? System.nanoTime() : 0;
        long now;
        long left = unit != null ? unit.toNanos(timeout) : 0;
        final long deadline = start + left;

        actor.monitorResetSkippedMessages();
        Object n = null;
        for (int i = 0;; i++) {
            if (actor.flightRecorder != null)
                actor.record(1, "Actor", "receive", "%s waiting for a message. %s", this, timeout > 0 ? "millis left: " + TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS) : "");

            mailbox.lock();
            n = mailbox.succ(n);

            if (n != null) {
                mailbox.unlock();
                final Object m = mailbox.value(n);
                if (m == currentMessage) {
                    mailbox.del(n);
                    continue;
                }

                actor.record(1, "Actor", "receive", "Received %s <- %s", this, m);
                actor.monitorAddMessage();
                try {
                    if (m instanceof LifecycleMessage) {
                        mailbox.del(n);
                        handleLifecycleMessage((LifecycleMessage) m);
                    } else {
                        final Message msg = (Message) m;
                        currentMessage = msg;
                        try {
                            T res = proc.process(msg);
                            if (res != null) {
                                if (mailbox.value(n) == msg) // another call to receive from within the processor may have deleted n
                                    mailbox.del(n);
                                return res;
                            }
                        } catch (Exception e) {
                            if (mailbox.value(n) == msg) // another call to receive from within the processor may have deleted n
                                mailbox.del(n);
                            throw e;
                        } finally {
                            currentMessage = null;
                        }
                        actor.monitorSkippedMessage();
                    }

                } catch (Exception e) {
                    if (mailbox.value(n) == m) // another call to receive from within the processor may have deleted n
                        mailbox.del(n);
                    throw e;
                }
            } else {
                try {
                    if (unit == null)
                        mailbox.await(i);
                    else if (timeout > 0) {
                        mailbox.await(i, left, TimeUnit.NANOSECONDS);

                        now = System.nanoTime();
                        left = deadline - now;
                        if (left <= 0) {
                            actor.record(1, "Actor", "receive", "%s timed out.", this);
                            throw new TimeoutException();
                        }
                    } else {
                        return null;
                    }
                } finally {
                    mailbox.unlock();
                }
            }
        }
    }

    public final <T> T receive(MessageProcessor<Message, T> proc) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, null, proc);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    public final <T> T tryReceive(MessageProcessor<Message, T> proc) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, TimeUnit.NANOSECONDS, proc);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    public static <M extends Message, Message> MessageProcessor<Message, M> ofType(final Class<M> type) {
        return new MessageProcessor<Message, M>() {
            @Override
            public M process(Message m) throws SuspendExecution, InterruptedException {
                return type.isInstance(m) ? type.cast(m) : null;
            }
        };
    }
    
    public final <M extends Message> M receive(long timeout, TimeUnit unit, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return receive(timeout, unit, (MessageProcessor<Message, M>)ofType(type));
    }

    public final <M extends Message> M receive(final Class<M> type) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, null, type);
        } catch (TimeoutException ex) {
            throw new AssertionError();
        }
    }

    protected void handleLifecycleMessage(LifecycleMessage m) {
        actor.handleLifecycleMessage(m);
    }
}
