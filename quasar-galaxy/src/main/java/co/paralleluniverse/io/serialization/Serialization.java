/*
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
package co.paralleluniverse.io.serialization;

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

    private Serialization(ByteArraySerializer bas) {
        this.bas = bas;
        this.ioss = (IOStreamSerializer) bas;
    }

    public Object read(byte[] buf) {
        return bas.read(buf);
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
