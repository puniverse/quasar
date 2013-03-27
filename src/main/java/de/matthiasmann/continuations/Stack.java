/*
 * Copyright (c) 2008, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.continuations;

import java.io.Serializable;

/**
 * Internal Class - DO NOT USE !
 * 
 * Needs to be public so that instrumented code can access it.
 * ANY CHANGE IN THIS CLASS NEEDS TO BE SYNCHRONIZED WITH {@link de.matthiasmann.continuations.instrument.InstrumentMethod}
 * 
 * @author Matthias Mann
 */
public final class Stack implements Serializable {

    private static final long serialVersionUID = 12786283751253L;
    
    private static final ThreadLocal<Stack> tls = new ThreadLocal<Stack>();
    
    /** sadly this need to be here */
    public static SuspendExecution exception_instance_not_for_user_code = SuspendExecution.instance;
    
    final Coroutine co;
    
    private int methodTOS = -1;
    private int[] method;
    
    private long[] dataLong;
    private Object[] dataObject;
    
    transient int curMethodSP;
    
    Stack(Coroutine co, int stackSize) {
        if(stackSize <= 0) {
            throw new IllegalArgumentException("stackSize");
        }
        this.co = co;
        this.method = new int[8];
        this.dataLong = new long[stackSize];
        this.dataObject = new Object[stackSize];
    }
    
    public static Stack getStack() {
        return tls.get();
    }
    
    static void setStack(Stack s) {
        tls.set(s);
    }
    
    /**
     * Called before a method is called.
     * @param entry the entry point in the method for resume
     * @param numSlots the number of required stack slots for storing the state
     */
    public final void pushMethodAndReserveSpace(int entry, int numSlots) {
        final int methodIdx = methodTOS;
        
        if(method.length - methodIdx < 2) {
            growMethodStack();
        }
        
        curMethodSP = method[methodIdx-1];
        int dataTOS = curMethodSP + numSlots;
        
        method[methodIdx] = entry;
        method[methodIdx+1] = dataTOS;
        
        //System.out.println("entry="+entry+" size="+size+" sp="+curMethodSP+" tos="+dataTOS+" nr="+methodIdx);
        
        if(dataTOS > dataObject.length) {
            growDataStack(dataTOS);
        }
    }
    
    /**
     * Called at the end of a method.
     * Undoes the effects of nextMethodEntry() and clears the dataObject[] array
     * to allow the values to be GCed.
     */
    public final void popMethod() {
        int idx = methodTOS;
        method[idx] = 0;
        int oldSP = curMethodSP;
        int newSP = method[idx-1];
        curMethodSP = newSP;
        methodTOS = idx-2;
        for(int i=newSP ; i<oldSP ; i++) {
            dataObject[i] = null;
        }
    }
    
    /**
     * called at the begin of a method
     * @return the entry point of this method
     */
    public final int nextMethodEntry() {
        int idx = methodTOS;
        curMethodSP = method[++idx];
        methodTOS = ++idx;
        return method[idx];
    }
    
    public static void push(int value, Stack s, int idx) {
        s.dataLong[s.curMethodSP + idx] = value;
    }
    public static void push(float value, Stack s, int idx) {
        s.dataLong[s.curMethodSP + idx] = Float.floatToRawIntBits(value);
    }
    public static void push(long value, Stack s, int idx) {
        s.dataLong[s.curMethodSP + idx] = value;
    }
    public static void push(double value, Stack s, int idx) {
        s.dataLong[s.curMethodSP + idx] = Double.doubleToRawLongBits(value);
    }
    public static void push(Object value, Stack s, int idx) {
        s.dataObject[s.curMethodSP + idx] = value;
    }

    public final int getInt(int idx) {
        return (int)dataLong[curMethodSP + idx];
    }
    public final float getFloat(int idx) {
        return Float.intBitsToFloat((int)dataLong[curMethodSP + idx]);
    }
    public final long getLong(int idx) {
        return dataLong[curMethodSP + idx];
    }
    public final double getDouble(int idx) {
        return Double.longBitsToDouble(dataLong[curMethodSP + idx]);
    }
    public final Object getObject(int idx) {
        return dataObject[curMethodSP + idx];
    }
    
    /** called when resuming a stack */
    final void resumeStack() {
        methodTOS = -1;
    }
    
    /* DEBUGGING CODE
    public void dump() {
        int sp = 0;
        for(int i=0 ; i<=methodTOS ; i++) {
            System.out.println("i="+i+" entry="+methodEntry[i]+" sp="+methodSP[i]);
            for(; sp < methodSP[i+1] ; sp++) {
                System.out.println("sp="+sp+" long="+dataLong[sp]+" obj="+dataObject[sp]);
            }
        }
    }
    */

    private void growDataStack(int required) {
        int newSize = dataObject.length;
        do {
            newSize *= 2;
        } while(newSize < required);
        
        dataLong = Util.copyOf(dataLong, newSize);
        dataObject = Util.copyOf(dataObject, newSize);
    }

    private void growMethodStack() {
        int newSize = method.length * 2;
        
        method = Util.copyOf(method, newSize);
    }
}
