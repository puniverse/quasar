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

import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.slf4j.Logger;

/**
 *
 * @author pron
 */
public abstract class BasicGenBehavior extends LocalActor<Object, Void> implements GenBehavior {
    private final Initializer initializer;
    private boolean run;

    public BasicGenBehavior(String name, Initializer initializer, Strand strand, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.initializer = initializer;
        this.run = true;
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public BasicGenBehavior(String name, Initializer initializer, int mailboxSize) {
        this(name, initializer, null, mailboxSize);
    }

    public BasicGenBehavior(String name, Initializer initializer) {
        this(name, initializer, null, -1);
    }

    public BasicGenBehavior(Initializer initializer, int mailboxSize) {
        this(null, initializer, null, mailboxSize);
    }

    public BasicGenBehavior(Initializer initializer) {
        this(null, initializer, null, -1);
    }

    public BasicGenBehavior(String name, int mailboxSize) {
        this(name, null, null, mailboxSize);
    }

    public BasicGenBehavior(String name) {
        this(name, null, null, -1);
    }

    public BasicGenBehavior(int mailboxSize) {
        this(null, null, null, mailboxSize);
    }

    public BasicGenBehavior() {
        this(null, null, null, -1);
    }
    //</editor-fold>
    
    @Override
    public void shutdown() {
        if (isInActor()) {
            run = false;
            getStrand().interrupt();
        } else
            send(new ShutdownMessage(LocalActor.self()));
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

    protected void init() throws SuspendExecution {
        if (initializer != null)
            initializer.init();
    }

    protected void terminate(Throwable cause) throws SuspendExecution {
        if (initializer != null)
            initializer.terminate(cause);
    }

    protected abstract Logger log();

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
    protected void handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ShutdownMessage) {
            shutdown();
        } else
            super.handleLifecycleMessage(m);
    }
}
