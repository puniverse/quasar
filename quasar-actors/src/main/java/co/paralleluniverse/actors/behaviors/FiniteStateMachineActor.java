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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.actors.SelectiveReceiveHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link BehaviorActor behavior} implementing a <i>finite-state-machine</i>.
 * The {@code FiniteStateMachineActor}'s behavior is implemented by overriding the {@link #initialState()} method to return an initial *state*
 * which then runs as the actor's body until it returns a next state, and so on until a state returns {@link #TERMINATE TERMINATE},
 * in which case the actor terminates.
 *
 * Initialization and termination behavior can be implemented by either 1) subclassing this class and overriding some or all of:
 * {@link #init() init}, {@link #terminate(java.lang.Throwable) terminate},
 * or 2) providing an instance of {@link Initializer} which implements these methods to the constructor.
 *
 * @author pron
 */
public class FiniteStateMachineActor extends BehaviorActor {
    protected static Object NULL_RETURN_VALUE = new Object();
    private static final Logger LOG = LoggerFactory.getLogger(FiniteStateMachineActor.class);
    /**
     * The termination state for the FSM
     */
    public static final SuspendableCallable<SuspendableCallable> TERMINATE = new SuspendableCallable<SuspendableCallable>() {
        @Override
        public SuspendableCallable run() throws SuspendExecution, InterruptedException {
            throw new AssertionError();
        }
    };
    private SuspendableCallable<SuspendableCallable> state;
    private final SelectiveReceiveHelper<Object> helper = new SelectiveReceiveHelper<>(this);

    /**
     * Creates a new FSM actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param strand        this actor's strand.
     * @param mailboxConfig this actor's mailbox settings.
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public FiniteStateMachineActor(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig) {
        super(name, initializer, strand, mailboxConfig);
        state = initialState();
        if (state == null)
            throw new NullPointerException();
    }

    /**
     * Creates a new FSM actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param strand        this actor's strand.
     * @param mailboxConfig this actor's mailbox settings.
     * @param initialState  the initial state; will be used instead of calling {@link #initialState() initialState()}.
     */
    public FiniteStateMachineActor(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig, SuspendableCallable<SuspendableCallable> initialState) {
        super(name, initializer, strand, mailboxConfig);
        state = initialState;
        if (state == null)
            throw new NullPointerException();
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    public static FiniteStateMachineActor currentFiniteStateMachineActor() {
        return (FiniteStateMachineActor) Actor.<Object, Void>currentActor();
    }

    @Override
    public Logger log() {
        return LOG;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Creates a new FSM actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public FiniteStateMachineActor(String name, Initializer initializer, MailboxConfig mailboxConfig) {
        this(name, initializer, null, mailboxConfig);
    }

    /**
     * Creates a new FSM actor
     *
     * @param name        the actor name (may be {@code null}).
     * @param initializer an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public FiniteStateMachineActor(String name, Initializer initializer) {
        this(name, initializer, null, null);
    }

    /**
     * Creates a new FSM actor
     *
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public FiniteStateMachineActor(Initializer initializer, MailboxConfig mailboxConfig) {
        this(null, initializer, null, mailboxConfig);
    }

    /**
     * Creates a new FSM actor
     *
     * @param initializer an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public FiniteStateMachineActor(Initializer initializer) {
        this(null, initializer, null, null);
    }

    /**
     * Creates a new FSM actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig this actor's mailbox settings.
     */
    public FiniteStateMachineActor(String name, MailboxConfig mailboxConfig) {
        this(name, null, null, mailboxConfig);
    }

    /**
     * Creates a new FSM actor
     *
     * @param name the actor name (may be {@code null}).
     */
    public FiniteStateMachineActor(String name) {
        this(name, null, null, null);
    }

    /**
     * Creates a new FSM actor
     *
     * @param mailboxConfig this actor's mailbox settings.
     */
    public FiniteStateMachineActor(MailboxConfig mailboxConfig) {
        this(null, null, null, mailboxConfig);
    }

    /**
     * Creates a new FSM actor
     */
    public FiniteStateMachineActor() {
        this(null, null, null, null);
    }
    //</editor-fold>

    /**
     * Returns this finite-state-machine actor's initial state; the default implementation returns {@link #TERMINATE TERMINATE}.
     * @return this finite-state-machine actor's initial state
     */
    protected SuspendableCallable<SuspendableCallable> initialState() {
        return TERMINATE;
    }

    @Override
    protected final void behavior() throws InterruptedException, SuspendExecution {
        while (isRunning()) {
            if (state == null)
                throw new NullPointerException();
            if (state == TERMINATE)
                break;
            checkCodeSwap();
            state = state.run();
        }
    }

    /**
     * Performs a selective receive. This method blocks until a message that is {@link MessageProcessor#process(java.lang.Object) selected} by
     * the given {@link MessageProcessor} is available in the mailbox, and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param proc performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}
     * @throws InterruptedException
     */
    public final SuspendableCallable<SuspendableCallable> receive(MessageProcessor<Object, SuspendableCallable<SuspendableCallable>> proc) throws SuspendExecution, InterruptedException {
        return helper.receive(proc);
    }

    /**
     * Performs a selective receive. This method blocks (but for no longer than the given timeout) until a message that is
     * {@link MessageProcessor#process(java.lang.Object) selected} by the given {@link MessageProcessor} is available in the mailbox,
     * and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param timeout the duration to wait for a matching message to arrive.
     * @param unit    timeout's time unit.
     * @param proc    performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if the timeout expired.
     * @throws InterruptedException
     */
    public final SuspendableCallable<SuspendableCallable> receive(long timeout, TimeUnit unit, MessageProcessor<Object, SuspendableCallable<SuspendableCallable>> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        return helper.receive(timeout, unit, proc);
    }

    /**
     * Performs a selective receive. This method blocks (but for no longer than the given timeout) until a message that is
     * {@link MessageProcessor#process(java.lang.Object) selected} by the given {@link MessageProcessor} is available in the mailbox,
     * and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param proc    performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if the timeout expired.
     * @throws InterruptedException
     */
    public final SuspendableCallable<SuspendableCallable> receive(Timeout timeout, MessageProcessor<Object, SuspendableCallable<SuspendableCallable>> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        return helper.receive(timeout, proc);
    }

    /**
     * Tries to perform a selective receive. If a message {@link MessageProcessor#process(java.lang.Object) selected} by
     * the given {@link MessageProcessor} is immediately available in the mailbox, returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * This method never blocks.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param proc performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if no message was slected.
     */
    public final SuspendableCallable<SuspendableCallable> tryReceive(MessageProcessor<Object, SuspendableCallable<SuspendableCallable>> proc) {
        return helper.tryReceive(proc);
    }

    /**
     * Performs a selective receive based on type. This method blocks until a message of the given type is available in the mailbox,
     * and returns it.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param type the type of the messages to select
     * @return The next message of the wanted type.
     * @throws InterruptedException
     */
    public final <M> M receive(final Class<M> type) throws SuspendExecution, InterruptedException {
        return helper.receive(SelectiveReceiveHelper.ofType(type));
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param timeout the duration to wait for a matching message to arrive.
     * @param unit    timeout's time unit.
     * @param type    the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <M> M receive(long timeout, TimeUnit unit, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return helper.receive(timeout, unit, SelectiveReceiveHelper.ofType(type));
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param type    the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <M> M receive(Timeout timeout, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return helper.receive(timeout, SelectiveReceiveHelper.ofType(type));
    }

    /**
     * Tries to performs a selective receive based on type. If a message of the given type is immediately found in the mailbox, it is returned.
     * Otherwise this method returns {@code null}.
     * This method never blocks.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param type the type of the messages to select
     * @return The next message of the wanted type if immediately found; {@code null} otherwise.
     */
    public final <M> M tryReceive(final Class<M> type) {
        return helper.tryReceive(SelectiveReceiveHelper.ofType(type));
    }

    @Override
    protected Object readResolve() throws java.io.ObjectStreamException {
        Object x = super.readResolve();
        assert x == this;
        helper.setActor(this);
        return this;
    }
}
