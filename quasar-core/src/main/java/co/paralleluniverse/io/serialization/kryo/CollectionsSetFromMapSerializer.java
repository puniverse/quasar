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

import co.paralleluniverse.common.reflection.GetAccessDeclaredField;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.lang.reflect.Field;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.security.AccessController.doPrivileged;

/**
 *
 * @author pron
 */
class CollectionsSetFromMapSerializer extends Serializer<Set> {
    private static final Field mf;
    private static final Field sf;

    static {
        try {
            final Class<?> cl = Collections.newSetFromMap(new HashMap()).getClass();
            mf = doPrivileged(new GetAccessDeclaredField(cl, "m"));
            sf = doPrivileged(new GetAccessDeclaredField(cl, "s"));
        } catch (PrivilegedActionException e) {
            throw new AssertionError(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public CollectionsSetFromMapSerializer() {
        setImmutable(true);
    }

    @Override
    public void write(Kryo kryo, Output output, Set object) {
        try {
            kryo.writeClassAndObject(output, mf.get(object));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Set read(Kryo kryo, Input input, Class<Set> type) {
        try {
            final Map m = (Map) kryo.readClassAndObject(input);
            final Set s = Collections.newSetFromMap(Collections.EMPTY_MAP); // must be created with an empty map
            mf.set(s, m);
            sf.set(s, m.keySet());
            return s;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
