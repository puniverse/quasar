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
package co.paralleluniverse.io.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Externalizable;
import java.io.IOException;

/**
 *
 * @author pron
 */
public class ExternalizableKryoSerializer<T extends Externalizable> extends Serializer<T> {
    @Override
    public void write(Kryo kryo, Output output, T obj) {
        try {
            obj.writeExternal(KryoUtil.asObjectOutput(output, kryo));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> type) {
        try {
            T obj = kryo.newInstance(type);
            obj.readExternal(KryoUtil.asObjectInput(input, kryo));
            return obj;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }
}
