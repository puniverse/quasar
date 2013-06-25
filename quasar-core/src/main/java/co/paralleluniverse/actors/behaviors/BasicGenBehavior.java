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
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import org.slf4j.Logger;

/**
 *
 * @author pron
 */
public abstract class BasicGenBehavior extends LocalActor<Object, Void> implements GenBehavior, java.io.Serializable {
    private final Initializer initializer;
    private boolean run;

    public BasicGenBehavior(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig) {
        super(strand, name, mailboxConfig);
        this.initializer = initializer;
        this.run = true;
    }
    
    protected abstract RemoteBasicGenBehavior getRemote(RemoteActor remote);

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public BasicGenBehavior(String name, Initializer initializer, MailboxConfig mailboxConfig) {
        this(name, initializer, null, mailboxConfig);
    }

    public BasicGenBehavior(String name, Initializer initializer) {
        this(name, initializer, null, null);
    }

    public BasicGenBehavior(Initializer initializer, MailboxConfig mailboxConfig) {
        this(null, initializer, null, mailboxConfig);
    }

    public BasicGenBehavior(Initializer initializer) {
        this(null, initializer, null, null);
    }

    public BasicGenBehavior(String name, MailboxConfig mailboxConfig) {
        this(name, null, null, mailboxConfig);
    }

    public BasicGenBehavior(String name) {
        this(name, null, null, null);
    }

    public BasicGenBehavior(MailboxConfig mailboxConfig) {
        this(null, null, null, mailboxConfig);
    }

    public BasicGenBehavior() {
        this(null, null, null, null);
    }
    //</editor-fold>

    @Override
    public void shutdown() {
        if (isInActor()) {
            log().debug("Shutdown requested.");
            run = false;
            getStrand().interrupt();
        } else {
            try {
                final ShutdownMessage message = new ShutdownMessage(LocalActor.self());
                sendOrInterrupt(message);
            } catch (QueueCapacityExceededException e) {
                final Strand strand = getStrand();
                if (strand != null)
                    strand.interrupt();
            }
        }
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
    protected void handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ShutdownMessage) {
            shutdown();
        } else
            super.handleLifecycleMessage(m);
    }

    @Override
    protected Object writeReplace() throws java.io.ObjectStreamException {
        final RemoteActor remote = (RemoteActor)super.writeReplace();
        return getRemote(remote);
    }
}
