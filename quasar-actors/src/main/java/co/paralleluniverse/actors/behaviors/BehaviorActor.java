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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.slf4j.Logger;

/**
 *
 * @author pron
 */
public abstract class BehaviorActor extends Actor<Object, Void> implements java.io.Serializable {
    private final Initializer initializer;
    private boolean run;

    protected BehaviorActor(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig) {
        super(strand, name, mailboxConfig);
        this.initializer = initializer;
        this.run = true;
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    @Override
    protected Behavior makeRef(ActorRef<Object> ref) {
        return new Behavior.Local(ref);
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
    public Behavior spawn(FiberScheduler scheduler) {
        return (Behavior) super.spawn(scheduler);
    }

    @Override
    public Behavior spawn() {
        return (Behavior) super.spawn();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public BehaviorActor(String name, Initializer initializer, MailboxConfig mailboxConfig) {
        this(name, initializer, null, mailboxConfig);
    }

    public BehaviorActor(String name, Initializer initializer) {
        this(name, initializer, null, null);
    }

    public BehaviorActor(Initializer initializer, MailboxConfig mailboxConfig) {
        this(null, initializer, null, mailboxConfig);
    }

    public BehaviorActor(Initializer initializer) {
        this(null, initializer, null, null);
    }

    public BehaviorActor(String name, MailboxConfig mailboxConfig) {
        this(name, null, null, mailboxConfig);
    }

    public BehaviorActor(String name) {
        this(name, null, null, null);
    }

    public BehaviorActor(MailboxConfig mailboxConfig) {
        this(null, null, null, mailboxConfig);
    }

    public BehaviorActor() {
        this(null, null, null, null);
    }
    //</editor-fold>

    protected void shutdown() {
        verifyInActor();
        log().debug("Shutdown requested.");
        run = false;
        getStrand().interrupt();
    }

    protected Initializer getInitializer() {
        return initializer;
    }

    protected void onStart() throws InterruptedException, SuspendExecution {
        init();
    }

    protected void onTerminate(Throwable cause) throws InterruptedException, SuspendExecution {
        log().info("{} shutting down.", this);
        terminate(cause);
    }

    protected void init() throws InterruptedException, SuspendExecution {
        if (initializer != null)
            initializer.init();
    }

    protected void terminate(Throwable cause) throws SuspendExecution {
        if (initializer != null)
            initializer.terminate(cause);
    }

    public abstract Logger log();

    protected void behavior() throws InterruptedException, SuspendExecution {
        while (isRunning()) {
            final Object m1 = receive();
            handleMessage(m1);
        }
    }

    protected void handleMessage(Object message) throws InterruptedException, SuspendExecution {
    }

    public boolean isRunning() {
        return run;
    }

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

    @Override
    protected Object handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ShutdownMessage)
            shutdown();
        else
            super.handleLifecycleMessage(m);
        return null;
    }
}
