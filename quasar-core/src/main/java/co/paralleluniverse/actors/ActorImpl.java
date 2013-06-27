/*
 * Quasar: lightweight threads and actors for the JVM.
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

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.SendChannel;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author pron
 */
public abstract class ActorImpl<Message> implements Actor<Message>, SendChannel<Message>, java.io.Serializable {
    static final long serialVersionUID = 894359345L;
    //
    private static final int MAX_SEND_RETRIES = 10;
    //
    private volatile Object name;
    private final SendChannel<Object> mailbox;
    private final LifecycleListener lifecycleListener = new ActorLifecycleListener(this, null);
    protected transient final FlightRecorder flightRecorder;

    @Override
    public String toString() {
        return "Actor@" + (name != null ? name : Integer.toHexString(System.identityHashCode(this)));
    }

    protected ActorImpl(Object name, SendChannel<Object> mailbox) {
        this.name = name;

        this.mailbox = mailbox;
        if (Debug.isDebug())
            this.flightRecorder = Debug.getGlobalFlightRecorder();
        else
            this.flightRecorder = null;
    }

    @Override
    public Object getName() {
        return name;
    }

    public final void setName(Object name) {
        if (this.name != null)
            throw new IllegalStateException("Actor " + this + " already has a name: " + this.name);
        this.name = name;
    }

    public static Object randtag() {
        return new BigInteger(80, ThreadLocalRandom.current()) {
            @Override
            public String toString() {
                return toString(16);
            }
        };
    }

    public static <Message> Actor<Message> getActor(Object name) {
        return ActorRegistry.getActor(name);
    }

    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    protected SendChannel<Object> mailbox() {
        return mailbox;
    }

    public SendChannel<Object> getMailbox() {
        return mailbox;
    }

    @Override
    public final void send(Message message) throws SuspendExecution {
        try {
            internalSend(message);
        } catch (QueueCapacityExceededException e) {
            onMailboxFull(message, e);
        }
    }

    public void sendOrInterrupt(Object message) {
        try {
            internalSendNonSuspendable(message);
        } catch (QueueCapacityExceededException e) {
            interrupt();
        }
    }

    @Override
    public void sendSync(Message message) throws SuspendExecution {
        send(message);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }
    

    /**
     * For internal use
     *
     * @param message
     */
    protected abstract void internalSend(Object message) throws SuspendExecution;

    protected abstract void internalSendNonSuspendable(Object message);

    protected abstract boolean isBackpressure();

    /**
     * This method is called <i>on the sender's strand</i> when the mailbox is full.
     *
     * @param e
     */
    protected void onMailboxFull(Message message, QueueCapacityExceededException e) throws SuspendExecution {
        if (isBackpressure()) {
            long sleepMillis = 1;
            for (int count = 1;; count++) {
                try {
                    mailbox.send(message);
                    break;
                } catch (QueueCapacityExceededException ex) {
                    try {
                        if (count > MAX_SEND_RETRIES) {
                            throwIn(e);
                            break;
                        } else if (count > 5) {
                            Strand.sleep(sleepMillis);
                            sleepMillis *= 5;
                        } else if (count > 4) {
                            Strand.yield();
                        }
                    } catch (InterruptedException ie) {
                        Strand.currentStrand().interrupt();
                    }
                }
            }
        } else {
            throwIn(e);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    protected abstract void throwIn(RuntimeException e);

    protected abstract void addLifecycleListener(LifecycleListener listener);

    protected abstract void removeLifecycleListener(LifecycleListener listener);

    protected LifecycleListener getLifecycleListener() {
        return lifecycleListener;
    }

    protected static class ActorLifecycleListener implements LifecycleListener, java.io.Serializable {
        private final ActorImpl observer;
        private final Object id;

        public ActorLifecycleListener(ActorImpl observer, Object id) {
            this.observer = observer;
            this.id = id;
        }

        @Override
        public void dead(Actor actor, Throwable cause) {
            observer.internalSendNonSuspendable(new ExitMessage(actor, cause, id));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.id);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ActorLifecycleListener))
                return false;

            return Objects.equals(observer, ((ActorLifecycleListener) obj).observer) && Objects.equals(id, ((ActorLifecycleListener) obj).id);
        }

        @Override
        public String toString() {
            return "ActorLifecycleListener{" + "observer: " + observer + ", id: " + id + '}';
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Recording">
    /////////// Recording ///////////////////////////////////
    protected final void record(int level, String clazz, String method, String format) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5);
    }

    protected final void record(int level, String clazz, String method, String format, Object... args) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, args);
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, null));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object... args) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, args));
    }

    private static FlightRecorderMessage makeFlightRecorderMessage(FlightRecorder.ThreadRecorder recorder, String clazz, String method, String format, Object[] args) {
        return new FlightRecorderMessage(clazz, method, format, args);
        //return ((FlightRecorderMessageFactory) recorder.getAux()).makeFlightRecorderMessage(clazz, method, format, args);
    }
    //</editor-fold>
}
