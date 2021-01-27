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

import co.paralleluniverse.actors.ActorLoader;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRefDelegate;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.RuntimeSuspendExecution;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.bytebuddy.ByteBuddy;
//import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 * Wraps a Java object in a {@link ServerActor} that exposes the object's methods as an interface and processes them in an actor
 * (on a dedicated strand).
 * <p>
 * You can either supply a target object to any of the public constructors, or extend this class and use the subclass itself as the target,
 * in which case use the protected constructors that don't take a {@code target} argument.
 * </p>
 * The interface(s) exposed must
 *
 * @author pron
 */
public class ProxyServerActor extends ServerActor<ProxyServerActor.Invocation, Object, ProxyServerActor.Invocation> {
    private final Class<?>[] interfaces;
    private Object target;
    private final boolean callOnVoidMethods;

    /**
     * Creates a new {@code ProxyServerActor}
     *
     * @param name              the actor's name (may be null)
     * @param strand            the actor's strand (may be null)
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; {@code target} must implement all these interfaces.
     */
    public ProxyServerActor(String name, Strand strand, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target, Class<?>[] interfaces) {
        super(name, null, 0L, null, strand, mailboxConfig);
        this.callOnVoidMethods = callOnVoidMethods;
        this.target = ActorLoader.getReplacementFor(target != null ? target : this);
        this.interfaces = interfaces != null ? Arrays.copyOf(interfaces, interfaces.length) : this.target.getClass().getInterfaces();
        if (this.interfaces == null)
            throw new IllegalArgumentException("No interfaces provided, and target of class " + this.target.getClass().getName() + " implements no interfaces");
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Creates a new {@code ProxyServerActor}
     *
     * @param name              the actor's name (may be null)
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; {@code target} must implement all these interfaces.
     */
    public ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(name, null, mailboxConfig, callOnVoidMethods, target, interfaces);
    }

    /**
     * Creates a new {@code ProxyServerActor} with the default mailbox settings.
     *
     * @param name              the actor's name (may be null)
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; {@code target} must implement all these interfaces.
     */
    public ProxyServerActor(String name, boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(name, null, null, callOnVoidMethods, target, interfaces);
    }

    /**
     * Creates a new {@code ProxyServerActor}
     *
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; {@code target} must implement all these interfaces.
     */
    public ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(null, null, mailboxConfig, callOnVoidMethods, target, interfaces);
    }

    /**
     * Creates a new {@code ProxyServerActor} with the default mailbox settings.
     *
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; {@code target} must implement all these interfaces.
     */
    public ProxyServerActor(boolean callOnVoidMethods, Object target, Class<?>... interfaces) {
        this(null, null, null, callOnVoidMethods, target, interfaces);
    }

    /**
     * Creates a new {@code ProxyServerActor}, which exposes all interfaces implemented by the given {@code target}.
     *
     * @param name              the actor's name (may be null)
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     */
    public ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target) {
        this(name, null, mailboxConfig, callOnVoidMethods, target, null);
    }

    /**
     * Creates a new {@code ProxyServerActor} with the default mailbox settings,
     * which exposes all interfaces implemented by the given {@code target}.
     *
     * @param name              the actor's name (may be null)
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     */
    public ProxyServerActor(String name, boolean callOnVoidMethods, Object target) {
        this(name, null, null, callOnVoidMethods, target, null);
    }

    /**
     * Creates a new {@code ProxyServerActor}, which exposes all interfaces implemented by the given {@code target}.
     *
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     */
    public ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods, Object target) {
        this(null, null, mailboxConfig, callOnVoidMethods, target, null);
    }

    /**
     * Creates a new {@code ProxyServerActor} with the default mailbox settings,
     * which exposes all interfaces implemented by the given {@code target}.
     *
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param target            the object implementing the actor's behaviors, on which the exposed interface methods will be called.
     */
    public ProxyServerActor(boolean callOnVoidMethods, Object target) {
        this(null, null, null, callOnVoidMethods, target, null);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls.
     *
     * @param name              the actor's name (may be null)
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; this class must implement all these interfaces.
     */
    protected ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods, Class<?>... interfaces) {
        this(name, null, mailboxConfig, callOnVoidMethods, null, interfaces);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls. The default mailbox settings will be used.
     *
     * @param name              the actor's name (may be null)
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; this class must implement all these interfaces.
     */
    protected ProxyServerActor(String name, boolean callOnVoidMethods, Class<?>... interfaces) {
        this(name, null, null, callOnVoidMethods, null, interfaces);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls. The default mailbox settings will be used.
     *
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; this class must implement all these interfaces.
     */
    protected ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods, Class<?>... interfaces) {
        this(null, null, mailboxConfig, callOnVoidMethods, null, interfaces);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls. The default mailbox settings will be used.
     *
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     * @param interfaces        the interfaces this actor's {@link ActorRef} will implement; this class must implement all these interfaces.
     */
    protected ProxyServerActor(boolean callOnVoidMethods, Class<?>... interfaces) {
        this(null, null, null, callOnVoidMethods, null, interfaces);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls, and all of the interfaces implemented by the subclass will be exposed by the {@link ActorRef}.
     *
     * @param name              the actor's name (may be null)
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     */
    protected ProxyServerActor(String name, MailboxConfig mailboxConfig, boolean callOnVoidMethods) {
        this(name, null, mailboxConfig, callOnVoidMethods, null, null);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls, and all of the interfaces implemented by the subclass will be exposed by the {@link ActorRef}.
     * The default mailbox settings will be used.
     *
     * @param name              the actor's name (may be null)
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     */
    protected ProxyServerActor(String name, boolean callOnVoidMethods) {
        this(name, null, null, callOnVoidMethods, null, null);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls, and all of the interfaces implemented by the subclass will be exposed by the {@link ActorRef}.
     *
     * @param mailboxConfig     this actor's mailbox settings.
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     */
    protected ProxyServerActor(MailboxConfig mailboxConfig, boolean callOnVoidMethods) {
        this(null, null, mailboxConfig, callOnVoidMethods, null, null);
    }

    /**
     * This constructor is for use by subclasses that are intended to serve as the target. This object will serve as the target
     * for the method calls, and all of the interfaces implemented by the subclass will be exposed by the {@link ActorRef}.
     * The default mailbox settings will be used.
     *
     * @param callOnVoidMethods whether calling void methods will block until they have completed execution
     */
    protected ProxyServerActor(boolean callOnVoidMethods) {
        this(null, null, null, callOnVoidMethods, null, null);
    }
    //</editor-fold>

    @Override
    protected final Server<Invocation, Object, Invocation> makeRef(ActorRef<Object> ref) {
        try {
            return getProxyClass(interfaces, callOnVoidMethods).getConstructor(ActorRef.class).newInstance(ref);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ConcurrentMap<Pair<Set<Class<?>>, Boolean>, Class<? extends Server>> classes = MapUtil.newConcurrentHashMap();
    private static final ObjectProxyServerImpl handler1 = new ObjectProxyServerImpl(true);
    private static final ObjectProxyServerImpl handler2 = new ObjectProxyServerImpl(false);

    private static Class<? extends Server> getProxyClass(Class<?>[] interfaces, boolean callOnVoidMethods) {
        final Pair<Set<Class<?>>, Boolean> key = new Pair(ImmutableSet.copyOf(interfaces), callOnVoidMethods);
        Class<? extends Server> clazz = classes.get(key);
        if (clazz == null) {
            clazz = new ByteBuddy() // http://bytebuddy.net/
                    .subclass(Server.class)
                    .implement(interfaces)
                    .implement(java.io.Serializable.class)
                    .method(isDeclaredBy(anyOf(interfaces))).intercept(InvocationHandlerAdapter.of(callOnVoidMethods ? handler1 : handler2))
                    .make()
                    .load(ProxyServerActor.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
            final Class<? extends Server> old = classes.putIfAbsent(key, clazz);
            if (old != null)
                clazz = old;
        }
        return clazz;
    }

    private static class ObjectProxyServerImpl implements InvocationHandler, java.io.Serializable {
        private final boolean callOnVoidMethods;

        private ObjectProxyServerImpl(boolean callOnVoidMethods) {
            this.callOnVoidMethods = callOnVoidMethods;
        }

        boolean isInActor(Server<Invocation, Object, Invocation> ref) {
            return Objects.equals(ref, LocalActor.self());
        }

        @Override
        @Suspendable
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            assert !method.getDeclaringClass().isAssignableFrom(ActorRefDelegate.class);

            assert !method.getDeclaringClass().isAssignableFrom(Server.class);            
//            final Class<?> cls = method.getDeclaringClass();
//            if (cls.isAssignableFrom(Server.class) || cls.isAssignableFrom(SendPort.class)) {
//                try {
//                    return method.invoke(ref, args);
//                } catch (InvocationTargetException e) {
//                    throw e.getCause();
//                }
//            }

            final Server<Invocation, Object, Invocation> ref = (Server<Invocation, Object, Invocation>) proxy;
            try {
                if (isInActor(ref)) {
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
            } catch (SuspendExecution e) {
                throw RuntimeSuspendExecution.of(e);
            }
        }

        protected Object readResolve() throws java.io.ObjectStreamException {
            return callOnVoidMethods ? handler1 : handler2;
        }
    }

    @Override
    protected void checkCodeSwap() throws SuspendExecution {
        verifyInActor();
        Object _target = ActorLoader.getReplacementFor(target);
        if (_target != target)
            log().info("Upgraded ProxyServerActor implementation: {}", _target);
        this.target = _target;
        super.checkCodeSwap();
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

    private static RuntimeException rethrow(Throwable t) throws Exception {
        if (t instanceof Exception)
            throw (Exception) t;
        if (t instanceof Error)
            throw (Error) t;
        throw new RuntimeException(t);
    }
}
