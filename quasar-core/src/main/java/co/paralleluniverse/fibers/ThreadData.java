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
package co.paralleluniverse.fibers;

import co.paralleluniverse.concurrent.util.ThreadAccess;
import co.paralleluniverse.concurrent.util.ThreadUtil;
import java.security.AccessControlContext;
import java.security.AccessController;

/**
 *
 * @author pron
 */
class ThreadData {
    private static final boolean MAINTAIN_ACCESS_CONTROL_CONTEXT = (System.getSecurityManager() != null);
    private transient ClassLoader contextClassLoader;
    private transient AccessControlContext inheritedAccessControlContext;
    private Object threadLocals;
    private Object inheritableThreadLocals;

    public ThreadData(Thread thread) {
        final Object inheritableTLs = ThreadAccess.getInheritableThreadLocals(thread);
        if (inheritableTLs != null)
            this.inheritableThreadLocals = ThreadAccess.createInheritedMap(inheritableTLs);
        this.contextClassLoader = ThreadAccess.getContextClassLoader(thread);
        if (MAINTAIN_ACCESS_CONTROL_CONTEXT)
            this.inheritedAccessControlContext = AccessController.getContext();
    }

    public void installDataInThread(Thread thread) {
        installThreadLocals(thread);
        installFiberContextClassLoader(thread);
        if (MAINTAIN_ACCESS_CONTROL_CONTEXT)
            installFiberInheritedAccessControlContext(thread);
    }

    public void restoreThreadData(Thread thread) {
        restoreThreadLocals(thread);
        restoreThreadContextClassLoader(thread);
        if (MAINTAIN_ACCESS_CONTROL_CONTEXT)
            restoreThreadInheritedAccessControlContext(thread);
    }

    /**
     * Also called by {@link TrueThreadLocal}.
     *
     * @param currentThread
     */
    void installThreadLocals(Thread currentThread) {
        switchThreadLocals(currentThread, true);
    }

    /**
     * Also called by {@link TrueThreadLocal}.
     *
     * @param currentThread
     */
    void restoreThreadLocals(Thread currentThread) {
        switchThreadLocals(currentThread, false);
    }

    private void switchThreadLocals(Thread currentThread, boolean install) {
        Object tmpThreadLocals = ThreadAccess.getThreadLocals(currentThread);
        Object tmpInheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(currentThread);

        ThreadAccess.setThreadLocals(currentThread, this.threadLocals);
        ThreadAccess.setInheritablehreadLocals(currentThread, this.inheritableThreadLocals);

        this.threadLocals = tmpThreadLocals;
        this.inheritableThreadLocals = tmpInheritableThreadLocals;
    }

    private void installFiberContextClassLoader(Thread currentThread) {
        final ClassLoader origContextClassLoader = ThreadAccess.getContextClassLoader(currentThread);
        ThreadAccess.setContextClassLoader(currentThread, contextClassLoader);
        this.contextClassLoader = origContextClassLoader;
    }

    private void restoreThreadContextClassLoader(Thread currentThread) {
        final ClassLoader origContextClassLoader = contextClassLoader;
        this.contextClassLoader = ThreadAccess.getContextClassLoader(currentThread);
        ThreadAccess.setContextClassLoader(currentThread, origContextClassLoader);
    }

    private void installFiberInheritedAccessControlContext(Thread currentThread) {
        final AccessControlContext origAcc = ThreadAccess.getInheritedAccessControlContext(currentThread);
        ThreadAccess.setInheritedAccessControlContext(currentThread, inheritedAccessControlContext);
        this.inheritedAccessControlContext = origAcc;
    }

    private void restoreThreadInheritedAccessControlContext(Thread currentThread) {
        final AccessControlContext origAcc = inheritedAccessControlContext;
        this.inheritedAccessControlContext = ThreadAccess.getInheritedAccessControlContext(currentThread);
        ThreadAccess.setInheritedAccessControlContext(currentThread, origAcc);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThreadData@").append(Integer.toHexString(System.identityHashCode(this))).append('{');
        if (threadLocals != null || inheritableThreadLocals != null || contextClassLoader != null || inheritedAccessControlContext != null) {
            sb.append("\n");
            if (threadLocals != null)
                sb.append("\tthreadLocals: ").append(ThreadUtil.getThreadLocalsString(threadLocals)).append('\n');
            if (inheritableThreadLocals != null)
                sb.append("\tinheritableThreadLocals: ").append(ThreadUtil.getThreadLocalsString(inheritableThreadLocals)).append('\n');
            if (contextClassLoader != null)
                sb.append("\tcontextClassLoader: ").append(contextClassLoader).append('\n');
            if (inheritedAccessControlContext != null)
                sb.append("\tinheritedAccessControlContext: ").append(inheritedAccessControlContext).append('\n');
            sb.append("}");
        } else
            sb.append("EMPTY");
        sb.append("}");
        return sb.toString();
    }
}
