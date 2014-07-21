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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author pron
 */
public final class JDKSerializer implements ByteArraySerializer, IOStreamSerializer {
    private final List<WriteReplaceEntry> writeReplace = new CopyOnWriteArrayList<>();

    public void registerWriteReplace(Class<?> clazz, WriteReplace wr) {
        writeReplace.add(new WriteReplaceEntry(clazz, wr));
    }

    @Override
    public byte[] write(Object object) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(writeReplace(object));
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object writeReplace(Object object) {
        for (WriteReplaceEntry entry : writeReplace) {
            if (entry.clazz.isInstance(object))
                return entry.writeReplace.writeReplace(object);
        }
        return object;
    }

    @Override
    public Object read(byte[] buf) {
        return read(buf, 0);
    }

    @Override
    public Object read(byte[] buf, int offset) {
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(buf, offset, buf.length - offset);
            final ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(OutputStream os, Object object) throws IOException {
        final ObjectOutput oo = toObjectOutput(os);
        oo.writeObject(object);
    }

    @Override
    public Object read(InputStream is) throws IOException {
        try {
            final ObjectInput oi = toObjectInput(is);
            return oi.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public static DataOutput toDataOutput(OutputStream os) {
        if (os instanceof DataOutput)
            return (DataOutput) os;
        return new DataOutputStream(os);
    }

    public static ObjectOutput toObjectOutput(OutputStream os) throws IOException {
        if (os instanceof ObjectOutput)
            return (ObjectOutput) os;
        return new ObjectOutputStream(os);
    }

    public static DataInput toDataInput(InputStream is) {
        if (is instanceof DataInput)
            return (DataInput) is;
        return new DataInputStream(is);
    }

    public static ObjectInput toObjectInput(InputStream is) throws IOException {
        if (is instanceof ObjectInput)
            return (ObjectInput) is;
        return new ObjectInputStream(is);
    }

    static class WriteReplaceEntry {
        final Class<?> clazz;
        final WriteReplace writeReplace;

        public WriteReplaceEntry(Class<?> clazz, WriteReplace writeReplace) {
            this.clazz = clazz;
            this.writeReplace = writeReplace;
        }
    }
}
