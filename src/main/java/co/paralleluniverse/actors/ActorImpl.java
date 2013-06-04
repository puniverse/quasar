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
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Mailbox;
import co.paralleluniverse.strands.channels.SendChannel;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author pron
 */
public abstract class ActorImpl<Message> implements Actor<Message>, java.io.Serializable {
    // TODO: This class may be redundant
    static final long serialVersionUID = 894359345L;
    private volatile Object name;
    final Mailbox<Object> mailbox;
    protected final FlightRecorder flightRecorder;

    @Override
    public String toString() {
        return "Actor@" + (name != null ? name : Integer.toHexString(System.identityHashCode(this)));
    }

    protected ActorImpl(Object name, Mailbox<Object> mailbox) {
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
        if(this.name != null)
            throw new IllegalStateException("Actor " + this + " already has a name: " + this.name);
        this.name = name;
    }

    public static Object randtag() {
        return new BigInteger(80, ThreadLocalRandom.current());
    }

    public static <Message, V> LocalActor<Message, V> getActor(Object name) {
        return (LocalActor<Message, V>)ActorRegistry.getActor(name);
    }

    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    Mailbox<Object> mailbox() {
        return mailbox;
    }

    public final SendChannel<Message> getMailbox() {
        return (Channel<Message>) mailbox;
    }

    @Override
    public final void send(Message message) {
        try {
            record(1, "Actor", "send", "Sending %s -> %s", message, this);
            if (mailbox.isOwnerAlive())
                mailbox.send(message);
            else
                record(1, "Actor", "send", "Message dropped. Owner not alive.");
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }

    @Override
    public final void sendSync(Message message) {
        try {
            record(1, "Actor", "sendSync", "Sending sync %s -> %s", message, this);
            if (mailbox.isOwnerAlive())
                mailbox.sendSync(message);
            else
                record(1, "Actor", "sendSync", "Message dropped. Owner not alive.");
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    protected abstract void throwIn(RuntimeException e);

    abstract void addLifecycleListener(LifecycleListener listener);

    abstract void removeLifecycleListener(LifecycleListener listener);

    abstract LifecycleListener getLifecycleListener();

    abstract Throwable getDeathCause();

    @Override
    public final Actor link(Actor other1) {
        final ActorImpl other = (ActorImpl)other1;
        record(1, "Actor", "link", "Linking actors %s, %s", this, other);
        if (!this.isDone() || !other.isDone()) {
            if (this.isDone())
                other.getLifecycleListener().dead(this, getDeathCause());
            else if (other.isDone())
                getLifecycleListener().dead(other, other.getDeathCause());
            else {
                addLifecycleListener(other.getLifecycleListener());
                other.addLifecycleListener(getLifecycleListener());
            }
        }
        return this;
    }

    @Override
    public final Actor unlink(Actor other1) {
        final ActorImpl other = (ActorImpl)other1;
        record(1, "Actor", "unlink", "Uninking actors %s, %s", this, other);
        removeLifecycleListener(other.getLifecycleListener());
        other.removeLifecycleListener(getLifecycleListener());
        return this;
    }

    @Override
    public final Object watch(Actor other1) {
        final ActorImpl other = (ActorImpl)other1;
        LifecycleListener listener = new LifecycleListener() {
            @Override
            public void dead(Actor actor, Throwable cause) {
                mailbox.send(new ExitMessage(actor, cause, this));
            }
        };
        record(1, "Actor", "monitor", "Actor %s to monitor %s (listener: %s)", this, other, listener);

        if (other.isDone())
            listener.dead(other, other.getDeathCause());
        else
            other.addLifecycleListener(listener);
        return listener;
    }

    @Override
    public final void unwatch(Actor other1, Object listener) {
        final ActorImpl other = (ActorImpl)other1;
        record(1, "Actor", "demonitor", "Actor %s to stop monitoring %s (listener: %s)", this, other, listener);
        other.removeLifecycleListener((LifecycleListener) listener);
    }
    //</editor-fold>

    //<editor-fold desc="Serialization">
    /////////// Serialization ///////////////////////////////////
    // If using Kryo, see what needs to be done: https://code.google.com/p/kryo/
    protected final Object writeReplace() throws java.io.ObjectStreamException {
        //return new SerializedActor(this);
        throw new UnsupportedOperationException();
    }

    protected static class SerializedActor implements java.io.Serializable {
        static final long serialVersionUID = 894359345L;
        private Actor actor;

        public SerializedActor(Actor actor) {
            this.actor = actor;
        }

        public SerializedActor() {
        }

        protected Object readResolve() throws java.io.ObjectStreamException {
            // return new Actor(...);
            throw new UnsupportedOperationException();
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
