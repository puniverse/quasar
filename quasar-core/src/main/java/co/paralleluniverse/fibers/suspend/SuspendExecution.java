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
package co.paralleluniverse.fibers.suspend;

/**
 * <p>
 * An exception used to initiate the control transfer.
 * Methods which are declared to throw this exception are "suspendable". This
 * exception must always be propagated and never be caught.</p>
 * <p>
 * Try/catch handlers are allowed:<br>
 * {@code try{ doSomething(); } catch(Throwable ex) { handleException(ex); } }</p>
 * <p>
 * Quasar instrumentation will enhance the bytecode of these methods to
 * support suspension and continuation of their execution; they will not catch {@code SuspendExecution}.</p>
 * <p>
 * This class is for internal Quasar use. 
 * Throwing an instance of this class or a subclass of it will break things in strange ways.</p>
 */
public final class SuspendExecution extends Exception { // InterruptedException {
    static final SuspendExecution PARK = new SuspendExecution();
    static final SuspendExecution YIELD = new SuspendExecution();

    public static boolean isPark(Throwable ex) {
        return ex == PARK;
    }

    public static boolean isYield(Throwable ex) {
        return ex == YIELD;
    }

    protected SuspendExecution() {
        super("Oops. Forgot to instrument a method. Run your program with -Dco.paralleluniverse.fibers.verifyInstrumentation=true to catch the culprit!");
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
