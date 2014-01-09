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

import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.ActorLoader;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRefDelegate;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.SendPort;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Wraps a Java object in a {@link ServerActor} that exposes the object's methods as an interface and processes them in an actor
 * (on a dedicated strand).
 *
 * @author pron
 */
public final class ProxyServerActor extends ServerActor<ProxyServerActor.Invocation, Object, ProxyServerActor.Invocation> {
    private final Class<?>[] interfaces;
    private Object target;
    private final boolean callOnVoidMethods;

    public ProxyServerActor(String name, Strand strand, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target, Class<?>[] interfaces) {
        super(name, null, 0L, null, strand, mailboxConfig);
        this.callOnVoidMethods = callOnVoidMethods;
        this.target = target != null ? target : this;
        this.interfaces = interfaces != null ? Arrays.copyOf(interfaces, interfaces.length) : target.getClass().getInterfaces();
        if (this.interfaces == null)
            throw new IllegalArgumentException("No interfaces provided, and target of class " + target.getClass().getName() + " implements no interfaces");
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(name, null, mailboxConfig, callOnVoidMethods, target, interfaces);
    }

    public ProxyServerActor(String name, boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(name, null, null, callOnVoidMethods, target, interfaces);
    }

    public ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(null, null, mailboxConfig, callOnVoidMethods, target, interfaces);
    }

    public ProxyServerActor(boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(null, null, null, callOnVoidMethods, target, interfaces);
    }

    public ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Class<?>... interfaces) {
        this(name, null, mailboxConfig, callOnVoidMethods, null, interfaces);
    }

    public ProxyServerActor(String name, boolean callOnVoidMethods, Class<?>... interfaces) {
        this(name, null, null, callOnVoidMethods, null, interfaces);
    }

    public ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods, Class<?>... interfaces) {
        this(null, null, mailboxConfig, callOnVoidMethods, null, interfaces);
    }

    public ProxyServerActor(boolean callOnVoidMethods, Class<?>... interfaces) {
        this(null, null, null, callOnVoidMethods, null, interfaces);
    }

    public ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target) {
        this(name, null, mailboxConfig, callOnVoidMethods, target, null);
    }

    public ProxyServerActor(String name, boolean callOnVoidMethods, Object target) {
        this(name, null, null, callOnVoidMethods, target, null);
    }

    public ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target) {
        this(null, null, mailboxConfig, callOnVoidMethods, target, null);
    }

    public ProxyServerActor(boolean callOnVoidMethods, Object target) {
        this(null, null, null, callOnVoidMethods, target, null);
    }

    public ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods) {
        this(name, null, mailboxConfig, callOnVoidMethods, null, null);
    }

    public ProxyServerActor(String name, boolean callOnVoidMethods) {
        this(name, null, null, callOnVoidMethods, null, null);
    }

    public ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods) {
        this(null, null, mailboxConfig, callOnVoidMethods, null, null);
    }

    public ProxyServerActor(boolean callOnVoidMethods) {
        this(null, null, null, callOnVoidMethods, null, null);
    }
    //</editor-fold>

    @Override
    protected Server<Invocation, Object, Invocation> makeRef(ActorRef<Object> ref) {
        return (Server<Invocation, Object, Invocation>) makeProxyRef(super.makeRef(ref));
    }

    private Object makeProxyRef(Server<Invocation, Object, Invocation> ref) {
        final boolean local = ref instanceof LocalBehavior;
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                combine(interfaces, local ? standardLocalInterfaces : standardInterfaces),
                new ObjectProxyServerImpl(local ? this : null,
                ref,
                callOnVoidMethods));
    }
    private static Class<?>[] standardInterfaces = new Class[]{
        Server.class,
        ActorRef.class,
        Behavior.class,
        SendPort.class,
        ActorRefDelegate.class,};
    private static Class<?>[] standardLocalInterfaces = new Class[]{
        Server.class,
        ActorRef.class,
        Behavior.class,
        SendPort.class,
        ActorBuilder.class,
        Joinable.class,
        LocalBehavior.class,
        ActorRefDelegate.class,};

    private static class ObjectProxyServerImpl implements InvocationHandler, java.io.Serializable {
        private transient final ProxyServerActor actor;
        private final boolean callOnVoidMethods;
        private final Server<Invocation, Object, Invocation> ref;

        ObjectProxyServerImpl(ProxyServerActor actor, Server<Invocation, Object, Invocation> ref, boolean callOnVoidMethods) {
            this.actor = actor;
            this.ref = ref;
            this.callOnVoidMethods = callOnVoidMethods;
        }

        boolean isInActor() {
            return Objects.equals(ref, LocalActor.self());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws SuspendExecution, Throwable {
            final Class<?> cls = method.getDeclaringClass();
            if (cls == ActorRefDelegate.class && method.getName().equals("getRef"))
                return ref;
            if (proxy instanceof LocalBehavior && method.getName().equals("writeReplace"))
                return actor.makeProxyRef((Server<Invocation, Object, Invocation>) ((LocalBehavior) ref).writeReplace());
            if (Arrays.asList(standardLocalInterfaces).contains(cls)) {
                try {
                    return method.invoke(ref, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
            if (cls == Object.class) {
                switch (method.getName()) {
                    case "hashCode":
                        return Objects.hashCode(ref);
                    case "equals":
                        if (!(args[0] instanceof ActorRef))
                            return false;
                        return ActorUtil.equals(ref, (ActorRef) args[0]);
                    case "toString":
                        return "ObjectProxyServer{" + ref.toString() + "}";
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            if (isInActor()) {
                try {
                    return method.invoke(ServerActor.currentServerActor(), args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            } else {
                final Invocation m = new Invocation(method, args, false);
                if (callOnVoidMethods || (method.getReturnType() != void.class && method.getReturnType() != Void.class))
                    return ref.call(m);
                else {
                    ref.cast(m);
                    return null;
                }
            }
        }
    }

    @Override
    protected void checkCodeSwap() {
        verifyInActor();
        Object _target = ActorLoader.getReplacementFor(target);
        if(_target != target)
            log().info("Upgraded ProxyServerActor implementation: {}", _target);
        this.target = _target;
    }
    
    @Override
    protected Object handleCall(ActorRef<?> from, Object id, Invocation m) throws Exception, SuspendExecution {
        try {
            Object res = m.invoke(target);
            return res == null ? NULL_RETURN_VALUE : res;
        } catch (InvocationTargetException e) {
            assert !(e.getCause() instanceof SuspendExecution);
            log().error("handleCall: Invocation " + m + " has thrown an exception.", e.getCause());
            throw rethrow(e.getCause());
        }
    }

    @Override
    protected void handleCast(ActorRef<?> from, Object id, Invocation m) throws SuspendExecution {
        try {
            m.invoke(target);
        } catch (InvocationTargetException e) {
            assert !(e.getCause() instanceof SuspendExecution);
            log().error("handleCast: Invocation " + m + " has thrown an exception.", e.getCause());
        }
    }

    protected static class Invocation implements java.io.Serializable {
        private final Method method;
        private final Object[] params;

        public Invocation(Method method, List<Object> params) {
            this.method = method;
            this.params = params.toArray(new Object[params.size()]);
        }

        public Invocation(Method method, Object... params) {
            this(method, params, false);
        }

        Invocation(Method method, Object[] params, boolean copy) {
            this.method = method;
            this.params = copy ? Arrays.copyOf(params, params.length) : params;
        }

        public Method getMethod() {
            return method;
        }

        public List<Object> getParams() {
            return Collections.unmodifiableList(Arrays.asList(params));
        }

        Object invoke(Object target) throws SuspendExecution, InvocationTargetException {
            try {
                return method.invoke(target, params);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public String toString() {
            return method.toString() + Arrays.toString(params);
        }
    }

    private static Class[] combine(Class[] ifaces1, Class[] ifaces2) {
        final Class[] ifaces = new Class[ifaces1.length + ifaces2.length];
        System.arraycopy(ifaces1, 0, ifaces, 0, ifaces1.length);
        System.arraycopy(ifaces2, 0, ifaces, ifaces1.length, ifaces2.length);
        return ifaces;
    }

    private static RuntimeException rethrow(Throwable t) throws Exception {
        if (t instanceof Exception)
            throw (Exception) t;
        if (t instanceof Error)
            throw (Error) t;
        throw new RuntimeException(t);
    }
}
