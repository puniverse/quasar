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
package co.paralleluniverse.fibers;

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Strand.State;
import javax.management.ConstructorParameters;
//import javax.management.openmbean.CompositeData;

/**
 * Current information about a fiber returned by {@link FibersMXBean}.
 *
 * @author pron
 */
public class FiberInfo {
//    public static FiberInfo from(CompositeData cd) {
//        CompositeData[] stcd = (CompositeData[]) cd.get("stackTrace");
//        StackTraceElement[] stackTrace = new StackTraceElement[stcd.length];
//        for (int i = 0; i < stcd.length; i++) {
//            stackTrace[i] = new StackTraceElement(
//                    (String)stcd[i].get("className"), 
//                    (String)stcd[i].get("methodName"), 
//                    (String)stcd[i].get("fileName"), 
//                    (Integer)stcd[i].get("lineNumber"));
//        }
//        return new FiberInfo(
//                ((Long) cd.get("id")).longValue(),
//                (String) cd.get("name"),
//                State.valueOf((String) cd.get("state")),
//                (String) cd.get("blocker"),
//                stackTrace);
//    }
    private final long id;
    private final String name;
    private final State state;
    private final Object blocker;
    private final String blockerName;
    private final StackTraceElement[] stackTrace;

    public FiberInfo(long id, String name, State state, Object blocker, StackTraceElement[] stackTrace) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.blocker = blocker;
        this.blockerName = null;
        this.stackTrace = stackTrace;
    }

    @ConstructorParameters({"id", "name", "state", "blocker", "stackTrace"})
    public FiberInfo(long id, String name, State state, String blockerName, StackTraceElement[] stackTrace) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.blockerName = blockerName;
        this.blocker = null;
        this.stackTrace = stackTrace;
    }

    /**
     * The fiber's ID.
     */
    public long getId() {
        return id;
    }

    /**
     * The fiber's name.
     */
    public String getName() {
        return name;
    }

    /**
     * The fiber's current {@link State state}.
     */
    public State getState() {
        return state;
    }

//    public Object getBlockerObject() {
//        return blocker;
//    }
    /**
     * The fiber's current {@link Strand#getBlocker() blocker}.
     */
    public String getBlocker() {
        if (blocker == null)
            return blockerName;
        else
            return blocker.toString();
    }

    /**
     * The fiber's current call stack.
     */
    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
