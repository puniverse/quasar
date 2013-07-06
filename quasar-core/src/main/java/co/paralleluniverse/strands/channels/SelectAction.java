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
package co.paralleluniverse.strands.channels;

/**
 *
 * @author pron
 */
public final class SelectAction<Message> {
    private volatile Selector selector;
    private final boolean isData;
    final Selectable<Message> port;
    int index;
    private Message item;
    private volatile boolean done;
    Object token;

    SelectAction(Selector selector, int index, Selectable<Message> port, Message message) {
        this.selector = selector;
        this.index = index;
        this.port = port;
        this.item = message;
        this.isData = message != null;
    }

    SelectAction(Selectable<Message> port, Message message) {
        this(null, -1, port, message);
    }

    Selector selector() {
        return selector;
    }

    void setSelector(Selector selector) {
        assert this.selector == null;
        this.selector = selector;
    }
    

    public Message message() {
        return item;
    }

    void setIndex(int index) {
        this.index = index;
    }
    
    public int index() {
        return index;
    }

    boolean isData() {
        return isData;
    }

    void setItem(Message item) {
        this.item = item;
        this.done = true;
    }

    public boolean isDone() {
        return done;
    }

    boolean lease() {
        if (selector == null)
            return true;
        return selector.lease();
    }

    void returnLease() {
        if (selector != null)
            selector.returnLease();
    }

    void won() {
        if (selector != null)
            selector.setWinner(this);
    }

    @Override
    public String toString() {
        return "SelectAction{" + (isData ? ("send " + item + " to") : "receive from") + " " + port
                + (isDone() ? (" " + (isData ? "done" : (" -> " + item))) : "") + '}' 
                + " " + selector
                ;
    }
}
