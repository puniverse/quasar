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
import java.io.NotSerializableException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 *
 * @author pron
 */
class ReferenceSerializer extends Serializer<Reference<?>> {
    public ReferenceSerializer() {
        setImmutable(true);
    }

    @Override
    public void write(Kryo kryo, Output output, Reference<?> r) {
        if (r instanceof PhantomReference)
            throw new RuntimeException(new NotSerializableException(r.getClass().getName()));
        final boolean strong = !(r instanceof WeakReference || r instanceof SoftReference);
        kryo.writeClassAndObject(output, strong ? r.get() : null);
    }

    @Override
    public Reference<?> read(Kryo kryo, Input input, Class<Reference<?>> type) {
        assert !PhantomReference.class.isAssignableFrom(type);
        final boolean strong = !(WeakReference.class.isAssignableFrom(type) || SoftReference.class.isAssignableFrom(type));
        final Object val = kryo.readClassAndObject(input);
        assert val == null || !strong;
        try {
            return (Reference<?>) type.getConstructor(Object.class).newInstance(val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
