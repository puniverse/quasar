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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorLoader;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import org.slf4j.Logger;

/**
 * A general behavior-actor class, extended by all behaviors. Behaviors are actor templates encapsulating common useful actor patterns.
 * This provides standard, sane, actor lifecycle methods, as well as other useful services (like a logger object).
 *
 * @author pron
 */
public abstract class BehaviorActor extends Actor<Object, Void> implements java.io.Serializable {
    private Initializer initializer;
    private boolean run;

    /**
     * Creates a new behavior actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param strand        this actor's strand.
     * @param mailboxConfig this actor's mailbox settings.
     */
    protected BehaviorActor(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig) {
        super(strand, name, mailboxConfig);
        this.initializer = ActorLoader.getReplacementFor(initializer);
        this.run = true;
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    @Override
    protected Behavior makeRef(ActorRef<Object> ref) {
        return new Behavior(ref);
    }

    @Override
    public Behavior ref() {
        return (Behavior) super.ref();
    }

    @Override
    protected Behavior self() {
        return ref();
    }

    @Override
    public Behavior spawn(StrandFactory sf) {
        return (Behavior) super.spawn(sf);
    }

    @Override
    public Behavior spawn(FiberFactory ff) {
        return (Behavior) super.spawn(ff);
    }

    @Override
    public Behavior spawn() {
        return (Behavior) super.spawn();
    }

    @Override
    public Behavior spawnThread() {
        return (Behavior) super.spawnThread();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Creates a new behavior actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public BehaviorActor(String name, Initializer initializer, MailboxConfig mailboxConfig) {
        this(name, initializer, null, mailboxConfig);
    }

    /**
     * Creates a new behavior actor.
     *
     * @param name        the actor name (may be {@code null}).
     * @param initializer an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public BehaviorActor(String name, Initializer initializer) {
        this(name, initializer, null, null);
    }

    /**
     * Creates a new behavior actor.
     *
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public BehaviorActor(Initializer initializer, MailboxConfig mailboxConfig) {
        this(null, initializer, null, mailboxConfig);
    }

    /**
     * Creates a new behavior actor.
     *
     * @param initializer an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public BehaviorActor(Initializer initializer) {
        this(null, initializer, null, null);
    }

    /**
     * Creates a new behavior actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig this actor's mailbox settings.
     */
    public BehaviorActor(String name, MailboxConfig mailboxConfig) {
        this(name, null, null, mailboxConfig);
    }

    /**
     * Creates a new behavior actor.
     *
     * @param name the actor name (may be {@code null}).
     */
    public BehaviorActor(String name) {
        this(name, null, null, null);
    }

    /**
     * Creates a new behavior actor.
     *
     * @param mailboxConfig this actor's mailbox settings.
     */
    public BehaviorActor(MailboxConfig mailboxConfig) {
        this(null, null, null, mailboxConfig);
    }

    /**
     * Creates a new behavior actor.
     */
    public BehaviorActor() {
        this(null, null, null, null);
    }
    //</editor-fold>

    /**
     * Causes this actor to shut down.
     */
    protected void shutdown() {
        verifyInActor();
        log().debug("Shutdown requested.");
        run = false;
        getStrand().interrupt();
    }

    /**
     * The {@link Initializer initializer} passed at construction which performs initialization and termination.
     */
    protected Initializer getInitializer() {
        return initializer;
    }

    /**
     * This method is called by the {@link BehaviorActor} at the beginning of {@link #doRun()}.
     * By default, this method calls {@link #init()}.
     */
    protected void onStart() throws InterruptedException, SuspendExecution {
        init();
    }

    /**
     * This method is called by the {@link BehaviorActor} at the end of {@link #doRun()}.
     * By default, this method calls {@link #terminate(Throwable) terminate()}.
     */
    protected void onTerminate(Throwable cause) throws InterruptedException, SuspendExecution {
        log().info("{} shutting down.", this);
        terminate(cause);
    }

    /**
     * Called by {@link #onStart() onStart} to initialize the actor. By default, this method calls {@link #getInitializer() initializer}.{@link Initializer#init() init()}
     * if the initializer in non-null; otherwise it does nothing.
     */
    protected void init() throws InterruptedException, SuspendExecution {
        if (initializer != null)
            initializer.init();
    }

    /**
     * Called by {@link #onTerminate(Throwable) onTerminate} to terminate the actor. By default, this method calls {@link #getInitializer() initializer}.{@link Initializer#terminate(java.lang.Throwable) terminate}
     * if the initializer in non-null; otherwise it does nothing.
     */
    protected void terminate(Throwable cause) throws SuspendExecution {
        if (initializer != null)
            initializer.terminate(cause);
    }

    /**
     * The {@link Logger} object associated with this actor.
     */
    public abstract Logger log();

    /**
     * Called by {@link #doRun()} as the body of the logic. By default, this implementation runs code similar to:
     * <pre> {@code
     *   while (isRunning())
     *       handleMessage(receive());
     * }</pre>
     */
    protected void behavior() throws InterruptedException, SuspendExecution {
        while (isRunning()) {
            final Object m1 = receive();
            handleMessage(m1);
        }
    }

    /**
     * Called by the default {@link #behavior()} method to handle each incoming message.
     * By default, this method does nothing.
     *
     * @param message the message received by the actor.
     */
    protected void handleMessage(Object message) throws InterruptedException, SuspendExecution {
    }

    @Override
    protected void checkCodeSwap() throws SuspendExecution {
        verifyInActor();
        Initializer _initializer = ActorLoader.getReplacementFor(initializer);
        if (_initializer != initializer)
            log().info("Upgraded behavior implementation: {}", _initializer);
        this.initializer = _initializer;
        super.checkCodeSwap();
    }

    public boolean isRunning() {
        return run;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation calls {@link #onStart()} when it begins, {@link #behavior()} for the body, and
     * {@link #onTerminate(Throwable) onTerminate()} upon termination. The implementation runs code similar to the following:</p>
     * <pre> {@code
     *   try {
     *       onStart();
     *       behavior();
     *   } catch (InterruptedException e) {
     *       if (shutdownCalled) {
     *           onTerminate(null);
     *           return null;
     *       } else {
     *           onTerminate(e);
     *           throw e;
     *       }
     *   } catch (Exception e) {
     *       log().info("Exception!", e);
     *       onTerminate(e);
     *       throw Exceptions.rethrow(e);
     *   }
     *   onTerminate(null);
     * }</pre>
     */
    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        try {
            onStart();
            behavior();
        } catch (InterruptedException e) {
            if (run == false) {
                onTerminate(null);
                return null;
            } else {
                onTerminate(e);
                throw e;
            }
        } catch (Exception e) {
            log().info("Exception!", e);
            onTerminate(e);
            throw Exceptions.rethrow(e);
        }

        onTerminate(null);
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation respects {@link ShutdownMessage} and, upon receiving it, calls {@link #shutdown() shutdown()}.</p>
     */
    @Override
    protected Object handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ShutdownMessage) {
            shutdown();
            return null;
        } else
            return super.handleLifecycleMessage(m);
    }
}
