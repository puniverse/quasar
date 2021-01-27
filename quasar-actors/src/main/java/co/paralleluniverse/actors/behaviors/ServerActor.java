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
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.behaviors.Server.ServerRequest;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link BehaviorActor behavior} implementing a <i>server</i> that responds to request messages.
 * The {@code ServerActor}'s behavior is implemented by either 1) subclassing this class and overriding some or all of:
 * {@link #init() init}, {@link #terminate(java.lang.Throwable) terminate},
 * {@link #handleCall(ActorRef, Object, Object) handleCall}, {@link #handleCast(ActorRef, Object, Object) handleCast},
 * {@link #handleInfo(Object) handleInfo}, and {@link #handleTimeout() handleTimeout};
 * or 2) providing an instance of {@link ServerHandler} which implements these methods to the constructor.
 *
 * @author pron
 */
public class ServerActor<CallMessage, V, CastMessage> extends BehaviorActor {
    protected static Object NULL_RETURN_VALUE = new Object();
    private static final Logger LOG = LoggerFactory.getLogger(ServerActor.class);
    private TimeUnit timeoutUnit;
    private long timeout;

    /**
     * Creates a new server actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param server        an optional delegate object that implements this server actor's behavior if this class is not subclassed. May be {@code null}.
     * @param timeout       the duration after which, if a request has not been received, the {@link #handleTimeout()} method will be called.
     * @param unit          {@code timeout}'s time unit. {@code null} if no timeout is to be set.
     * @param strand        this actor's strand.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public ServerActor(String name, ServerHandler<CallMessage, V, CastMessage> server, long timeout, TimeUnit unit, Strand strand, MailboxConfig mailboxConfig) {
        super(name, server, strand, mailboxConfig);
        this.timeoutUnit = timeout > 0 ? unit : null;
        this.timeout = timeout;
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    @Override
    protected Server<CallMessage, V, CastMessage> makeRef(ActorRef<Object> ref) {
        return new Server<CallMessage, V, CastMessage>(ref);
    }

    @Override
    public Server<CallMessage, V, CastMessage> ref() {
        return (Server<CallMessage, V, CastMessage>) super.ref();
    }

    @Override
    protected Server<CallMessage, V, CastMessage> self() {
        return ref();
    }

    @Override
    public Server<CallMessage, V, CastMessage> spawn(StrandFactory sf) {
        return (Server<CallMessage, V, CastMessage>) super.spawn(sf);
    }

    @Override
    public Server<CallMessage, V, CastMessage> spawn(FiberFactory ff) {
        return (Server<CallMessage, V, CastMessage>) super.spawn(ff);
    }

    @Override
    public Server<CallMessage, V, CastMessage> spawn() {
        return (Server<CallMessage, V, CastMessage>) super.spawn();
    }

    @Override
    public Server<CallMessage, V, CastMessage> spawnThread() {
        return (Server<CallMessage, V, CastMessage>) super.spawnThread();
    }

    public static <CallMessage, V, CastMessage> ServerActor<CallMessage, V, CastMessage> currentServerActor() {
        return (ServerActor<CallMessage, V, CastMessage>) Actor.<Object, Void>currentActor();
    }

    @Override
    public Logger log() {
        return LOG;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Creates a new server actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param server        an optional delegate object that implements this server actor's behavior if this class is not subclassed. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public ServerActor(String name, ServerHandler<CallMessage, V, CastMessage> server, MailboxConfig mailboxConfig) {
        this(name, server, -1, null, null, mailboxConfig);
    }

    /**
     * Creates a new server actor
     *
     * @param name   the actor name (may be {@code null}).
     * @param server an optional delegate object that implements this server actor's behavior if this class is not subclassed. May be {@code null}.
     */
    public ServerActor(String name, ServerHandler<CallMessage, V, CastMessage> server) {
        this(name, server, -1, null, null, null);
    }

    /**
     * Creates a new server actor
     *
     * @param server        an optional delegate object that implements this server actor's behavior if this class is not subclassed. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public ServerActor(ServerHandler<CallMessage, V, CastMessage> server, MailboxConfig mailboxConfig) {
        this(null, server, -1, null, null, mailboxConfig);
    }

    /**
     * Creates a new server actor
     *
     * @param server an optional delegate object that implements this server actor's behavior if this class is not subclassed. May be {@code null}.
     */
    public ServerActor(ServerHandler<CallMessage, V, CastMessage> server) {
        this(null, server, -1, null, null, null);
    }

    /**
     * Creates a new server actor
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig this actor's mailbox settings.
     */
    public ServerActor(String name, MailboxConfig mailboxConfig) {
        this(name, null, -1, null, null, mailboxConfig);
    }

    /**
     * Creates a new server actor
     *
     * @param name the actor name (may be {@code null}).
     */
    public ServerActor(String name) {
        this(name, null, -1, null, null, null);
    }

    /**
     * Creates a new server actor
     *
     * @param mailboxConfig this actor's mailbox settings.
     */
    public ServerActor(MailboxConfig mailboxConfig) {
        this(null, null, -1, null, null, mailboxConfig);
    }

    /**
     * Creates a new server actor
     */
    public ServerActor() {
        this(null, null, -1, null, null, null);
    }
    //</editor-fold>

    /**
     * The {@link ServerHandler} passed at construction, or {@code null} if none was set.
     */
    protected ServerHandler<CallMessage, V, CastMessage> server() {
        return (ServerHandler<CallMessage, V, CastMessage>) getInitializer();
    }

    @Override
    protected final void behavior() throws InterruptedException, SuspendExecution {
        while (isRunning()) {
            checkCodeSwap();
            Object m1 = receive(timeout, timeoutUnit);
            if (m1 == null)
                handleTimeout();
            else
                handleMessage(m1);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method implements the {@code ServerActor} behavior, dispatching message to {@link #handleCall(ActorRef, Object, Object) handleCall},
     * {@link #handleCast(ActorRef, Object, Object) handleCast} or {@link #handleInfo(Object) handleInfo} as appropriate.</p>
     */
    @Override
    protected void handleMessage(Object m) throws InterruptedException, SuspendExecution {
        if (m instanceof ServerRequest) {
            ServerRequest r = (ServerRequest) m;
            switch (r.getType()) {
                case CALL:
                    try {
                        final V res = handleCall((ActorRef<V>) r.getFrom(), r.getId(), (CallMessage) r.getMessage());
                        if (res != null)
                            reply((ActorRef<V>) r.getFrom(), r.getId(), res == NULL_RETURN_VALUE ? null : res);
                    } catch (Exception e) {
                        replyError((ActorRef<V>) r.getFrom(), r.getId(), e);
                    }
                    break;

                case CAST:
                    handleCast((ActorRef<V>) r.getFrom(), r.getId(), (CastMessage) r.getMessage());
                    break;
            }
        } else
            handleInfo(m);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    /**
     * Sets a duration after which, if a request has not been received, the {@link #handleTimeout()} method will be called.
     * The time count is reset after every received message. This method will be triggered multiple times if a message is not received
     * for a period of time longer than multiple timeout durations.
     *
     * @param timeout the timeout duration
     * @param unit    {@code timeout}'s time unit; {@code null} if the timeout is to be unset.
     */
    public final void setTimeout(long timeout, TimeUnit unit) {
        verifyInActor();
        this.timeoutUnit = timeout > 0 ? unit : null;
        this.timeout = timeout;
    }

    /**
     * Called to handle a synchronous request (one waiting for a response).
     * <p>
     * By default, this method calls {@link #server() server}.{@link ServerHandler#handleCall(ActorRef, Object, Object) handleCall} if a server object was supplied
     * at construction time. Otherwise, it throws an {@link UnsupportedOperationException}, which will be sent back to the requester.</p>
     * <ul>
     * <li>If this method returns a non-null value, it will be sent back to the sender of the request wrapped by an {@link ErrorResponseMessage};
     * if the request was sent via {@link Server#call(Object) Server.call} (which is how it's usually done), this value will be returned
     * by the {@link Server#call(java.lang.Object) call} method.</li>
     * <li>If this method throws an exception, it will be sent back to the sender of the request wrapped by an {@link ErrorResponseMessage};
     * if the request was sent via {@link Server#call(Object) Server.call}, the exception will be thrown by the {@link Server#call(java.lang.Object) call}
     * method, possibly wrapped in a {@link RuntimeException}.</li>
     * <li>If this method returns {@code null}, then a reply is not immediately sent, and the {@link Server#call(java.lang.Object) call} method
     * will remain blocked until a reply is sent manually with {@link #reply(ActorRef, Object, Object) reply} or
     * {@link #replyError(ActorRef, Object, Throwable) replyError}.</li>
     * </ul>
     *
     * @param from the sender of the request
     * @param id   the request's unique id
     * @param m    the request
     * @return a value that will be sent as a response to the sender of the request.
     * @throws Exception if thrown, it will be sent back to the sender of the request.
     */
    protected V handleCall(ActorRef<?> from, Object id, CallMessage m) throws Exception, SuspendExecution {
        if (server() != null)
            return server().handleCall(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
    }

    /**
     * Replies with a result to a call request, if the {@link #handleCall(ActorRef, Object, Object) handleCall} method returned null.
     * <p>
     * If the request has been sent by a call to {@link Server#call(Object) Server.call} (which is how it's usually done), the
     * {@code result} argument will be the value returned by {@link Server#call(Object) call}.</p>
     * <p>
     * This method can only be called by this actor.</p>
     * <p>
     * Internally this method uses a {@link ValueResponseMessage} to send the reply.</p>
     *
     * @param to    the actor we're responding to (the request's sender)
     * @param id    the request's identifier
     * @param value the result of the request
     */
    public final void reply(ActorRef<?> to, Object id, V value) throws SuspendExecution {
        verifyInActor();
        ((ActorRef) to).send(new ValueResponseMessage<V>(id, value));
    }

    /**
     * Replies with an exception to a call request, if the {@link #handleCall(ActorRef, Object, Object) handleCall} method returned null.
     * If the request has been sent by a call to {@link Server#call(Object) Server.call} (which is how it's usually done), the
     * {@code e} argument will be the exception thrown by {@link Server#call(Object) call} (possibly wrapped by a {@link RuntimeException}).
     * <p>
     * This method can only be called by this actor.</p>
     * <p>
     * Internally this method uses an {@link ErrorResponseMessage} to send the reply.</p>
     *
     * @param to    the actor we're responding to (the request's sender)
     * @param id    the request's identifier
     * @param error the error the request has caused
     */
    public final void replyError(ActorRef<?> to, Object id, Throwable error) throws SuspendExecution {
        verifyInActor();
        ((ActorRef) to).send(new ErrorResponseMessage(id, error));
    }

    /**
     * Called to handle an asynchronous request (one that does not for a response).
     * <p>
     * By default, this method calls {@link #server() server}.{@link ServerHandler#handleCast(ActorRef, Object, Object)  handleCast} if a server object was supplied
     * at construction time. Otherwise, it throws an {@link UnsupportedOperationException}, which will result in this actor's death, unless caught.</p>
     *
     * @param from the sender of the request
     * @param id   the request's unique id
     * @param m    the request
     */
    protected void handleCast(ActorRef<?> from, Object id, CastMessage m) throws SuspendExecution {
        if (server() != null)
            server().handleCast(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
    }

    /**
     * Called to handle any message sent to this actor that is neither a {@link #handleCall(ActorRef, Object, Object) call} nor a {@link #handleCast(ActorRef, Object, Object) cast}.
     * <p>
     * By default, this method calls {@link #server() server}.{@link ServerHandler#handleInfo(Object) handleInfo} if a server object was supplied
     * at construction time. Otherwise, it does nothing.</p>
     *
     * @param m the message
     */
    protected void handleInfo(Object m) throws SuspendExecution {
        if (server() != null)
            server().handleInfo(m);
    }

    /**
     * Called whenever the timeout set with {@link #setTimeout(long, TimeUnit) setTimeout} or supplied at construction expires without any message
     * received. The countdown is reset after every received message. This method will be triggered multiple times if a message is not received
     * for a period of time longer than multiple timeout durations.
     * <p>
     * By default, this method calls {@link #server() server}.{@link ServerHandler#handleTimeout() handleTimeout} if a server object was supplied
     * at construction time. Otherwise, it does nothing.</p>
     */
    protected void handleTimeout() throws SuspendExecution {
        if (server() != null)
            server().handleTimeout();
    }
}
