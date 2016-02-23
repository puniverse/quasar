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
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import java.io.Serializable;

/**
 *
 * @author pron
 */
public class ThreadLocalSerializer extends Serializer<ThreadLocal<?>> {
    public static boolean PRINT_WARNINGS_ON_UNSERIALIZABLE_THREAD_LOCAL = false;

    static final class DEFAULT {}
    
    public ThreadLocalSerializer() {
        setImmutable(true);
    }

    @Override
    public void write(Kryo kryo, Output output, ThreadLocal<?> tl) {
        output.writeBoolean(tl instanceof InheritableThreadLocal);
        final Object val = tl.get();
        final int pos = output.position();
        try {
            kryo.writeClassAndObject(output, val);
        } catch (RuntimeException e) {
            output.setPosition(pos);
            kryo.writeObjectOrNull(output, null, DEFAULT.class);
        }
    }

    @Override
    public ThreadLocal<?> read(Kryo kryo, Input input, Class<ThreadLocal<?>> type) {
        final boolean inheritable = input.readBoolean();
        final ThreadLocal tl = inheritable ? new InheritableThreadLocal() : new ThreadLocal();

        final Class<?> clazz = kryo.readClass(input).getType();
        if (!clazz.equals(DEFAULT.class))
            tl.set(kryo.readObject(input, clazz));
        return tl;
    }

    private static boolean canSerialize(Kryo kryo, ThreadLocal<?> tl, Object val) {
        if (val == null)
            return true;
        if (val instanceof Serializable || kryo.getClassResolver().getRegistration(val.getClass()) != null)
            return true;
        if (val instanceof co.paralleluniverse.io.serialization.Serialization)
            return false;
        if (!kryo.getDefaultSerializer(val.getClass()).getClass().isAssignableFrom(FieldSerializer.class))
            return true;

        // If we can't serialize the ThreadLocal then we just deserialise it as null. In practice, TLS slots are
        // almost always filled out on demand with some sort of cached object, so this is often OK.
        if (PRINT_WARNINGS_ON_UNSERIALIZABLE_THREAD_LOCAL)
            System.err.println("WARNING: Cannot serialize ThreadLocal (" + tl + " = " + val + "), it will be restored as null.");

        return false;
    }
}
