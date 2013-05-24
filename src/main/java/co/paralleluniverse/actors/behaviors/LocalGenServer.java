/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.behaviors.GenServerHelper.GenServerMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LocalGenServer<Message, V> extends LocalActor<GenServerMessage<Message>, Void> implements GenServer<Message, V> {
    private final Server<Message, V> server;
    private long timeout; // nanos
    private boolean run;

    public LocalGenServer(String name, Server<Message, V> server, Strand strand, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.server = server;
        this.timeout = -1;
        this.run = true;
    }

    public LocalGenServer(String name, Server<Message, V> server, int mailboxSize) {
        this(name, server, null, mailboxSize);
    }

    public LocalGenServer(String name, Server<Message, V> server) {
        this(name, server, null, -1);
    }

    public LocalGenServer(Server<Message, V> server, int mailboxSize) {
        this(null, server, null, mailboxSize);
    }

    public LocalGenServer(Server<Message, V> server) {
        this(null, server, null, -1);
    }

    public LocalGenServer(String name, int mailboxSize) {
        this(name, null, null, mailboxSize);
    }

    public LocalGenServer(String name) {
        this(name, null, null, -1);
    }

    public LocalGenServer(int mailboxSize) {
        this(null, null, null, mailboxSize);
    }

    public LocalGenServer() {
        this(null, null, null, -1);
    }

    @Override
    public V call(Message m) throws InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m);
    }

    @Override
    public void cast(Message m) {
        GenServerHelper.cast(this, m);
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        try {
            init();
            while (run) {
                Object m1 = receive(timeout, TimeUnit.NANOSECONDS);
                if (m1 instanceof GenServerMessage) {
                    GenServerMessage<Message> m = (GenServerMessage<Message>) m1;
                    switch (m.getType()) {
                        case CALL:
                            V res = handleCall(m.getSender(), m.getMessage());
                            if (res != null)
                                m.getSender().send(res);
                            break;

                        case CAST:
                            handleCall(m.getSender(), m.getMessage());
                            break;
                    }
                } else {
                    handleInfo(m1);
                }
            }
            terminate(null);
            return null;
        } catch (Throwable e) {
            terminate(e);
            throw e;
        }
    }

    protected final void reply(Actor to, V message) {
        to.send(message);
    }

    protected final void setTimeout(long timeout, TimeUnit unit) {
        this.timeout = unit.toNanos(timeout);
    }

    protected final void stop() {
        run = false;
    }

    protected void init() {
        server.init();
    }
    
    protected V handleCall(Actor<V> from, Message m) {
        return server.handleCall(from, m);
    }

    protected void handleCast(Actor<V> from, Message m) {
        server.handleCast(from, m);
    }

    protected void handleInfo(Object m) {
        server.handleInfo(m);
    }

    protected void terminate(Throwable cause) {
        server.terminate(cause);
    }
}
