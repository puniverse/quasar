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

    public void installDataInThread(Thread currentThread) {
        installLocals(currentThread);
        installFiberContextClassLoader(currentThread);
        if (MAINTAIN_ACCESS_CONTROL_CONTEXT)
            installFiberInheritedAccessControlContext(currentThread);
    }

    public void restoreThreadData(Thread currentThread) {
        restoreThreadLocals(currentThread);
        restoreThreadContextClassLoader(currentThread);
        if (MAINTAIN_ACCESS_CONTROL_CONTEXT)
            restoreThreadInheritedAccessControlContext(currentThread);
    }

    /**
     * Also called by {@link TrueThreadLocal}.
     *
     * @param currentThread
     */
    void installLocals(Thread currentThread) {
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

//        if (isRecordingLevel(2)) {
//            record(2, "ThreadData", "switchThreadLocals", "fiberLocals: %s", ThreadUtil.getThreadLocalsString(install ? this.threadLocals : tmpThreadLocals));
//            record(2, "ThreadData", "switchThreadLocals", "inheritableFilberLocals: %s", ThreadUtil.getThreadLocalsString(install ? this.inheritableThreadLocals : tmpInheritableThreadLocals));
//        }

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
}
