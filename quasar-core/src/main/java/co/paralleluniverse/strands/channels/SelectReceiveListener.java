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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.suspend.SuspendExecution;

/**
 * A listener associated with a {@link Selector#receive(ReceivePort, SelectReceiveListener) receive SelectAction}, which is called if an only if
 * the associated action has succeeded.
 * 
 * @author pron
 */
public interface SelectReceiveListener<Message> extends SelectListener<Message> {
    void onReceive(Message m) throws SuspendExecution;
}
