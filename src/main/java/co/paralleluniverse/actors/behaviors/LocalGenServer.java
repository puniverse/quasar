/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.behaviors.GenServerHelper.GenServerRequest;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LocalGenServer<Message, V> extends LocalActor<Object, Void> implements GenServer<Message, V> {
    private final Server<Message, V> server;
    private long timeout; // nanos
    private boolean run;

    public LocalGenServer(String name, Server<Message, V> server, long timeout, TimeUnit unit, Strand strand, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.server = server;
        this.timeout = unit.toNanos(timeout);
        this.run = true;
    }

    public LocalGenServer(String name, Server<Message, V> server, int mailboxSize) {
        this(name, server, -1, null, null, mailboxSize);
    }

    public LocalGenServer(String name, Server<Message, V> server) {
        this(name, server, -1, null, null, -1);
    }

    public LocalGenServer(Server<Message, V> server, int mailboxSize) {
        this(null, server, -1, null, null, mailboxSize);
    }

    public LocalGenServer(Server<Message, V> server) {
        this(null, server, -1, null, null, -1);
    }

    public LocalGenServer(String name, int mailboxSize) {
        this(name, null, -1, null, null, mailboxSize);
    }

    public LocalGenServer(String name) {
        this(name, null, -1, null, null, -1);
    }

    public LocalGenServer(int mailboxSize) {
        this(null, null, -1, null, null, mailboxSize);
    }

    public LocalGenServer() {
        this(null, null, -1, null, null, -1);
    }

    @Override
    public V call(Message m) throws InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m);
    }

    @Override
    public V call(Message m, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m, timeout, unit);
    }

    @Override
    public void cast(Message m) {
        GenServerHelper.cast(this, m);
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        try {
            while (run) {
                Object m1 = receive(timeout, TimeUnit.NANOSECONDS);
                if (m1 instanceof GenServerRequest) {
                    GenServerRequest<Message> m = (GenServerRequest<Message>) m1;
                    switch (m.getType()) {
                        case CALL:
                            try {
                                final V res = handleCall((Actor<V>) m.getFrom(), m.getId(), m.getMessage());
                                if (res != null)
                                    reply((Actor<V>) m.getFrom(), m.getId(), res);
                            } catch (Exception e) {
                                replyError((Actor<V>) m.getFrom(), m.getId(), e);
                            }
                            break;

                        case CAST:
                            handleCast((Actor<V>) m.getFrom(), m.getId(), m.getMessage());
                            break;
                    }
                } else if (m1 == null)
                    handleTimeout();
                else
                    handleInfo(m1);
            }
            terminate(null);
            return null;
        } catch (Throwable e) {
            terminate(e);
            throw e;
        }
    }

    protected final void reply(Actor to, Object id, V message) {
        to.send(new GenValueResponseMessage<V>(id, message));
    }

    protected final void replyError(Actor to, Object id, Throwable error) {
        to.send(new GenErrorResponseMessage(id, error));
    }

    protected final void setTimeout(long timeout, TimeUnit unit) {
        this.timeout = unit.toNanos(timeout);
    }

    protected final void stop() {
        run = false;
    }

    @Override
    protected void init() {
        server.init();
    }

    protected V handleCall(Actor<V> from, Object id, Message m) {
        return server.handleCall(from, id, m);
    }

    protected void handleCast(Actor<V> from, Object id, Message m) {
        server.handleCast(from, id, m);
    }

    protected void handleInfo(Object m) {
        server.handleInfo(m);
    }

    protected void handleTimeout() {
        server.handleTimeout();
    }

    protected void terminate(Throwable cause) {
        server.terminate(cause);
    }
}
