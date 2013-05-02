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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/**
 *
 * @author pron
 */
public interface FiberByteChannel extends Channel /*ByteChannel*/ {

    int read(final ByteBuffer dst) throws IOException, SuspendExecution;
    int write(final ByteBuffer src) throws IOException, SuspendExecution;
}
