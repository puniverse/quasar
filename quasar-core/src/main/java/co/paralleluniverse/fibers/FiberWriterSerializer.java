/*
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 *
 * @author pron
 */
class FiberWriterSerializer extends Serializer<FiberWriter> {
    public FiberWriterSerializer() {
        setImmutable(true);
    }

    @Override
    public void write(Kryo kryo, Output output, FiberWriter fw) {
    }

    @Override
    public FiberWriter read(Kryo kryo, Input input, Class<FiberWriter> type) {
        return null;
    }
}
