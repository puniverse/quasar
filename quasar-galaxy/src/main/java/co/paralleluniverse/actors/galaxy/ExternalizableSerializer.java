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
package co.paralleluniverse.actors.galaxy;

import com.esotericsoftware.kryo.Serializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.nio.ByteBuffer;
import static co.paralleluniverse.actors.galaxy.KryoSerializer.KRYO;
import co.paralleluniverse.actors.galaxy.KryoSerializer.KryoObjectInputStream;
import co.paralleluniverse.actors.galaxy.KryoSerializer.KryoObjectOutputStream;
import co.paralleluniverse.common.io.ByteBufferInputStream;
import co.paralleluniverse.common.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 *
 * @author pron
 */
public class ExternalizableSerializer<T> extends Serializer<T> {
    @Override
    public void write(Kryo kryo, Output output, T object) {
        final Externalizable obj = (Externalizable) object;
        try {
            KryoObjectOutputStream oos = (KryoObjectOutputStream)output;
            obj.writeExternal(oos);
            oos.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> type) {
        try {
            KryoObjectInputStream ois = (KryoObjectInputStream)input;
            T obj = KRYO.newInstance(type);
            ((Externalizable) obj).readExternal(ois);
            return obj;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] writeExternalizableToBytes(Externalizable obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            KryoObjectOutputStream oos = new KryoObjectOutputStream(baos);
            obj.writeExternal(oos);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void writeExternalizableToBuffer(Externalizable obj, ByteBuffer buffer) {
        try {
            ByteBufferOutputStream bdos = new ByteBufferOutputStream(buffer);
            KryoObjectOutputStream oos = new KryoObjectOutputStream(bdos);
            obj.writeExternal(oos);
            oos.flush();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static <T extends Externalizable> T readExternalizableFromBytes(byte[] buffer, Class<T> type) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            KryoObjectInputStream ois = new KryoObjectInputStream(bais);
            T obj = KRYO.newInstance(type);
            obj.readExternal(ois);
            return obj;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public static <T extends Externalizable> T readExternalizableFromBuffer(ByteBuffer buffer, Class<T> type) {
        try {
            ByteBufferInputStream bdis = new ByteBufferInputStream(buffer);
            KryoObjectInputStream ois = new KryoObjectInputStream(bdis);
            T obj = KRYO.newInstance(type);
            obj.readExternal(ois);
            return obj;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
