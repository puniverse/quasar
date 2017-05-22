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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.Objects;

public abstract class ActorImpl<Message> implements java.io.Serializable {
    static final long serialVersionUID = 894359345L;
    //
    private static final int MAX_SEND_RETRIES = 10;
    //
    protected transient ActorRef<Message> ref; // can actually be set after onstruction by ActorRef.readResolve()
    private volatile String name;
    private final SendPort<Object> mailbox;
    private LifecycleListener lifecycleListener;
    protected transient final FlightRecorder flightRecorder;

    @Override
    public String toString() {
        return "Actor@" + (name != null ? name : Integer.toHexString(System.identityHashCode(this)));
    }

    protected ActorImpl(String name, SendPort<Object> mailbox, ActorRef<Message> ref) {
        this.name = name;

        this.mailbox = mailbox;
        this.flightRecorder = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
        this.ref = ref != null ? ref : new ActorRef<>(this);
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        if (this.name != null)
            throw new IllegalStateException("Actor " + this + " already has a name: " + this.name);
        this.name = name;
    }

    public ActorRef<Message> ref() {
        return ref;
    }

    void setRef(ActorRef<Message> ref) {
        this.ref = ref;
    }
    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    protected SendPort<Object> mailbox() {
        return mailbox;
    }

    public SendPort<Object> getMailbox() {
        return mailbox;
    }

    public void sendOrInterrupt(Object message) {
        try {
            internalSendNonSuspendable(message);
        } catch (QueueCapacityExceededException e) {
            interrupt();
        }
    }

    protected void sendSync(Message message) throws SuspendExecution {
        MutabilityTester.testMutability(message);
        try {
            internalSend(message);
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }

    public void close() {
        throw new UnsupportedOperationException();
    }

    public void close(Throwable t) {
        throw new UnsupportedOperationException();
    }

    protected abstract void interrupt();

    protected abstract boolean trySend(Message message);

    /**
     * For internal use
     *
     * @param message
     */
    protected abstract void internalSend(Object message) throws SuspendExecution;

    protected abstract void internalSendNonSuspendable(Object message);
    //</editor-fold>

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    protected abstract void throwIn(RuntimeException e);

    protected abstract void addLifecycleListener(LifecycleListener listener);

    protected abstract void removeLifecycleListener(LifecycleListener listener);
    
    protected abstract void removeObserverListeners(ActorRef actor);

    protected abstract void linked(ActorRef actor);

    protected abstract void unlinked(ActorRef actor);

    protected LifecycleListener getLifecycleListener() {
        if (lifecycleListener == null) // maybe benign race
            lifecycleListener = new ActorLifecycleListener(ref(), null);
        return lifecycleListener;
    }

    protected static class ActorLifecycleListener implements LifecycleListener, java.io.Serializable {
        private final ActorRef observer;
        private final Object id;

        public ActorLifecycleListener(ActorRef observer, Object id) {
            this.observer = observer;
            this.id = id;
        }

        @Override
        public void dead(ActorRef actor, Throwable cause) {
            observer.getImpl().internalSendNonSuspendable(new ExitMessage(actor, cause, id));
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

        @Override
        public Object getId() {
            return id;
        }

        public ActorRef getObserver() {
            return observer;
        }
    }
    //</editor-fold>

    static ActorImpl getActorRefImpl(ActorRef actor) {
        while (actor instanceof ActorRefDelegate)
            actor = ((ActorRefDelegate) actor).getRef();
        return actor.getImpl();
    }

    //<editor-fold defaultstate="collapsed" desc="Recording">
    /////////// Recording ///////////////////////////////////
    protected final boolean isRecordingLevel(int level) {
        if (flightRecorder == null)
            return false;
        final FlightRecorder.ThreadRecorder recorder = flightRecorder.get();
        if (recorder == null)
            return false;
        return recorder.recordsLevel(level);
    }

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
