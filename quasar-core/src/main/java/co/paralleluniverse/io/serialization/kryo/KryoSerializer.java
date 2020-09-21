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

import co.paralleluniverse.io.serialization.ByteArraySerializer;
import co.paralleluniverse.io.serialization.IOStreamSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is not thread-safe.
 *
 * @author pron
 */
public class KryoSerializer implements ByteArraySerializer, IOStreamSerializer {
    private static final Queue<Registration> registrations = new ConcurrentLinkedQueue<>();
    public final Kryo kryo;
    private Input input;
    private Output output;

    public KryoSerializer() {
        this.kryo = KryoUtil.newKryo();

        KryoUtil.registerCommonClasses(kryo);

        for (Registration r : registrations)
            register(r);
    }

    public Kryo getKryo() {
        return kryo;
    }

    public static void register(Class<?> type) {
        register(type, NULL_SERIALIZER, -1);
    }

    public static void register(Class<?> type, int id) {
        register(type, NULL_SERIALIZER, id);
    }

    public static void register(Class<?> type, Serializer<?> ser) {
        register(type, ser, -1);
    }

    public static void register(Class<?> type, Serializer<?> ser, int id) {
        registrations.add(new Registration(type, ser, id));
    }

    private Input getInput() {
        if (input == null)
            input = new Input(4096);
        return input;
    }

    private Output getOutput() {
        if (output == null)
            output = new Output(4096, -1);
        return output;
    }
    private static final Serializer<? super Object> NULL_SERIALIZER = new Serializer<>() {
        @Override
        public void write(Kryo kryo, Output output, Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object read(Kryo kryo, Input input, Class<Object> type) {
            throw new UnsupportedOperationException();
        }
    };

    private void register(Registration r) {
        if (r.getId() < 0 && r.getSerializer() == NULL_SERIALIZER)
            kryo.register(r.getType());
        else if (r.getId() < 0)
            kryo.register(r.getType(), r.getSerializer());
        else if (r.getSerializer() == NULL_SERIALIZER)
            kryo.register(r.getType(), r.getId());
        else
            kryo.register(r.getType(), r.getSerializer(), r.getId());
    }

    @Override
    public byte[] write(Object object) {
        final Output out = getOutput();
        out.clear();
        kryo.writeClassAndObject(out, object);
        out.flush();
        return out.toBytes();
    }

    @Override
    public Object read(byte[] buf) {
        return read(buf, 0);
    }

    @Override
    public Object read(byte[] buf, int offset) {
        final Input in = new Input(buf, offset, buf.length - offset);
        return kryo.readClassAndObject(in);
    }

    public <T> T read(byte[] buf, Class<T> type) {
        return read(buf, 0, type);
    }

    public <T> T read(byte[] buf, int offset, Class<T> type) {
        final Input in = new Input(buf, offset, buf.length - offset);
        return kryo.readObjectOrNull(in, type);
    }

    @Override
    public void write(OutputStream os, Object object) {
        final Output out = getOutput();
        out.clear();
        out.setOutputStream(os);
        kryo.writeClassAndObject(out, object);
        out.flush();
        out.setOutputStream(null);
    }

    @Override
    public Object read(InputStream is) throws IOException {
        final Input in = getInput();
        in.setInputStream(is);
        return kryo.readClassAndObject(in);
    }

    public <T> T read(InputStream is, Class<T> type) {
        final Input in = getInput();
        in.setInputStream(is);
        return kryo.readObjectOrNull(input, type);
    }
}
