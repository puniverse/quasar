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
package co.paralleluniverse.io.serialization;

import co.paralleluniverse.io.serialization.kryo.KryoSerializer;
import com.esotericsoftware.kryo.Kryo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author pron
 */
public final class Serialization {
    private static final boolean useJDKSerialization = Boolean.getBoolean("co.paralleluniverse.io.useJDKSerialization");
    private static final Serialization instance = useJDKSerialization ? new Serialization(new JDKSerializer()) : null;
    private static final ThreadLocal<Serialization> tlInstance = new ThreadLocal<Serialization>() {
        @Override
        protected Serialization initialValue() {
            return new Serialization(new KryoSerializer());
        }
    };

    private final ByteArraySerializer bas;
    private final IOStreamSerializer ioss;

    public static Serialization getInstance() {
        if (instance != null)
            return instance;
        else
            return tlInstance.get();
    }

    public static Serialization newInstance() {
        if (instance != null)
            return instance;
        else
            return new Serialization(new KryoSerializer());
    }

    public static Kryo getKryo() {
        return ((KryoSerializer) tlInstance.get().bas).getKryo();
    }

    private Serialization(ByteArraySerializer bas) {
        this.bas = bas;
        this.ioss = (IOStreamSerializer) bas;
    }

    public Object read(byte[] buf) {
        return read(buf, 0);
    }

    public Object read(byte[] buf, int offset) {
        return bas.read(buf, offset);
    }

    public byte[] write(Object object) {
        return bas.write(object);
    }

    public Object read(InputStream is) throws IOException {
        return ioss.read(is);
    }

    public void write(OutputStream os, Object object) throws IOException {
        ioss.write(os, object);
    }
}
