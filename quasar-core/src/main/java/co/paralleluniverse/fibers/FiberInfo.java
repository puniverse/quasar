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
package co.paralleluniverse.fibers;

import co.paralleluniverse.strands.Strand.State;
import java.beans.ConstructorProperties;

/**
 *
 * @author pron
 */
public class FiberInfo {
    private long id;
    private String name;
    private State state;
    private Object blocker;
    private StackTraceElement[] stackTrace;

    @ConstructorProperties({"id", "name", "state", "blocker", "stackTrace"})
    public FiberInfo(long id, String name, State state, Object blocker, StackTraceElement[] stackTrace) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.blocker = blocker;
        this.stackTrace = stackTrace;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }

    public Object getBlocker() {
        return blocker;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
