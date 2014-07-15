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
class ThreadLocalSerializer extends Serializer<ThreadLocal<?>> {
    public ThreadLocalSerializer() {
        setImmutable(true);
    }

    @Override
    public void write(Kryo kryo, Output output, ThreadLocal<?> tl) {
        output.writeBoolean(tl instanceof InheritableThreadLocal);
        final Object val = tl.get();
        final boolean reset = shouldReset(kryo, tl, val);
        kryo.writeObject(output, new ThreadLocalValue(val, reset));
    }

    @Override
    public ThreadLocal<?> read(Kryo kryo, Input input, Class<ThreadLocal<?>> type) {
        final boolean itl = input.readBoolean();
        final ThreadLocalValue tlv = (ThreadLocalValue) kryo.readObject(input, ThreadLocalValue.class);

        final ThreadLocal tl = itl ? new InheritableThreadLocal() : new ThreadLocal();
        if (!tlv.reset)
            tl.set(tlv.val);
        return tl;
    }

    private static boolean shouldReset(Kryo kryo, ThreadLocal<?> tl, Object val) {
        if (val == null)
            return false;
        if (val instanceof Serializable || kryo.getClassResolver().getRegistration(val.getClass()) != null)
            return false;
        if (val instanceof co.paralleluniverse.io.serialization.Serialization
                || tl.getClass().getName().startsWith("org.gradle.")
                || val.getClass().getName().startsWith("org.gradle."))
            return true;
        if (!kryo.getDefaultSerializer(val.getClass()).getClass().isAssignableFrom(FieldSerializer.class))
            return false;
        System.err.println("WARNING: cannot serialize ThreadLocal (" + tl + " = " + val + ")");
        return true;
    }

    static class ThreadLocalValue implements Serializable {
        boolean reset;
        Object val;

        public ThreadLocalValue(Object val, boolean reset) {
            this.val = reset ? null : val;
            this.reset = reset;
        }
    }
}
