/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    private final LocalActor<Message, ?> actor;
    private Message currentMessage; // this works because channel is single-consumer

    public SelectiveReceiveHelper(LocalActor<Message, ?> actor) {
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
    public final Message receive(long timeout, TimeUnit unit, MessageProcessor<Message> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        assert LocalActor.self() == null || LocalActor.self() == actor;

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
                            if (proc.process(msg)) {
                                if (mailbox.value(n) == msg) // another call to receive from within the processor may have deleted n
                                    mailbox.del(n);
                                return msg;
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

    public final Message receive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, null, proc);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    public final Message tryReceive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, TimeUnit.NANOSECONDS, proc);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    public static <Message, T> MessageProcessor<Message> ofType(final Class<T> type) {
        return new MessageProcessor<Message>() {
            @Override
            public boolean process(Message m) throws SuspendExecution, InterruptedException {
                return (type.isInstance(m));
            }
        };
    }
    
    public final <T extends Message> T receive(long timeout, TimeUnit unit, final Class<T> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return type.cast(receive(timeout, unit, (MessageProcessor<Message>)ofType(type)));
    }

    public final <T extends Message> T receive(final Class<T> type) throws SuspendExecution, InterruptedException {
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
