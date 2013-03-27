/*
 * Copyright (c) 2008-2013, Matthias Mann
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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A Coroutine based iterator
 * 
 * @param <E> the element class that the Iterator returns
 * @author Matthias Mann
 */
public abstract class CoIterator<E> implements Iterator<E>, Serializable {

    private static final long serialVersionUID = 351278561539L;
    
    private final Coroutine co;
    
    private E element;
    private boolean hasElement;
    
    protected CoIterator() {
        co = new Coroutine(new DelegateExecute());
    }

    public boolean hasNext() {
        while(!hasElement && co.getState() != Coroutine.State.FINISHED) {
            co.run();
        }
        return hasElement;
    }

    public E next() {
        if(!hasNext()) {
            throw new NoSuchElementException();
        }
        E result = element;
        hasElement = false;
        element = null;
        return result;
    }

    /**
     * Always throws UnsupportedOperationException.
     * @throws java.lang.UnsupportedOperationException always
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Produces the next value to be returned by the {@link #next} method.
     * 
     * @param element The value that should be returned by {@link #next}
     * @throws de.matthiasmann.continuations.SuspendExecution This method will suspend the execution
     */
    protected void produce(E element) throws SuspendExecution {
        if(hasElement) {
            throw new IllegalStateException("hasElement = true");
        }
        this.element = element;
        hasElement = true;
        Coroutine.yield();
    }
    
    /**
     * <p>This is the body of the Iterator. This method is executed as a
     * {@link Coroutine} to {@link #produce} the values of the Iterator.</p>
     * 
     * <p>Note that this method is suspended each time it calls produce. And if
     * the consumer does not consume all values of the Iterator then this 
     * method does not get the change to finish it's execution. This also
     * includes the finally blocks.</p>
     * 
     * <p>This method must only suspend by calling produce. Any other reason
     * for suspension will cause a busy loop in the Iterator.</p>
     * 
     * @throws de.matthiasmann.continuations.SuspendExecution
     */
    protected abstract void run() throws SuspendExecution;

    private class DelegateExecute implements CoroutineProto, Serializable {
        private static final long serialVersionUID = 12784529515412L;
    
        public void coExecute() throws SuspendExecution {
            CoIterator.this.run();
        }
    }
}
