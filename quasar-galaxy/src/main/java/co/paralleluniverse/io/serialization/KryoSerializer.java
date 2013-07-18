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
package co.paralleluniverse.io.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

/**
 *
 * @author pron
 */
public class KryoSerializer implements ByteArraySerializer, IOStreamSerializer {
    private static Queue<Registration> registrations = new ConcurrentLinkedQueue<Registration>();
    
    public final Kryo kryo;

    public KryoSerializer() {
        this.kryo = new ReplaceableObjectKryo();

        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());

        kryo.register(boolean[].class);
        kryo.register(byte[].class);
        kryo.register(short[].class);
        kryo.register(char[].class);
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(long[].class);
        kryo.register(double[].class);
        kryo.register(String[].class);
        kryo.register(int[][].class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.LinkedList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.LinkedHashMap.class);
        kryo.register(java.util.TreeMap.class);
        kryo.register(java.util.EnumMap.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(java.util.TreeSet.class);
        kryo.register(java.util.EnumSet.class);

        kryo.register(java.util.Arrays.asList("").getClass(), new ArraysAsListSerializer());
//        kryo.register(java.util.Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
//        kryo.register(java.util.Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
//        kryo.register(java.util.Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
//        kryo.register(java.util.Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
//        kryo.register(java.util.Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
//        kryo.register(java.util.Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
        kryo.register(java.util.GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(java.lang.reflect.InvocationHandler.class, new JdkProxySerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);
        kryo.addDefaultSerializer(Externalizable.class, new ExternalizableKryoSerializer());
        
        for (Registration r : registrations)
            register(r);
    }

    public static void register(Class type) {
        register(type, NULL_SERIALIZER, -1);
    }
    
    public static void register(Class type, int id) {
        register(type, NULL_SERIALIZER, id);
    }
    
    public static void register(Class type, Serializer ser) {
        register(type, ser, -1);
    }
    
    public static void register(Class type, Serializer ser, int id) {
        registrations.add(new Registration(type, ser, id));
    }
    
    private static Serializer NULL_SERIALIZER = new Serializer<Object>() {

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
        if(r.getId() < 0 && r.getSerializer() == NULL_SERIALIZER)
            kryo.register(r.getType());
        else if(r.getId() < 0)
            kryo.register(r.getType(), r.getSerializer());
        else if(r.getSerializer() == NULL_SERIALIZER)
            kryo.register(r.getType(), r.getId());
        else
            kryo.register(r.getType(), r.getSerializer(), r.getId());
    } 

    @Override
    public byte[] write(Object object) {
        final Output output = new KryoObjectOutputStream(512, -1);
        kryo.writeClassAndObject(output, object);
        output.flush();
        return output.toBytes();
    }

    @Override
    public Object read(byte[] buf) {
        final Input input = new KryoObjectInputStream(buf);
        return kryo.readClassAndObject(input);
    }

    public <T> T read(byte[] buffer, Class<T> type) {
        final Input input = new KryoObjectInputStream(buffer);
        return kryo.readObjectOrNull(input, type);
    }

    @Override
    public void write(OutputStream os, Object object) {
        final Output output = toKryoObjectOutputStream(os);
        kryo.writeClassAndObject(output, object);
        output.flush();
    }

    @Override
    public Object read(InputStream is) throws IOException {
        final Input input = toKryoObjectInputStream(is);
        return kryo.readClassAndObject(input);
    }

    public <T> T read(InputStream is, Class<T> type) {
        final Input input = toKryoObjectInputStream(is);
        return kryo.readObjectOrNull(input, type);
    }

    private KryoObjectOutputStream toKryoObjectOutputStream(OutputStream os) {
        if (os instanceof KryoObjectOutputStream)
            return (KryoObjectOutputStream) os;
        return new KryoObjectOutputStream(os);
    }

    private KryoObjectInputStream toKryoObjectInputStream(InputStream is) {
        if (is instanceof KryoObjectInputStream)
            return (KryoObjectInputStream) is;
        return new KryoObjectInputStream(is);
    }

    class KryoObjectOutputStream extends Output implements DataOutput, ObjectOutput {
        public KryoObjectOutputStream() {
        }

        public KryoObjectOutputStream(int bufferSize) {
            super(bufferSize);
        }

        public KryoObjectOutputStream(int bufferSize, int maxBufferSize) {
            super(bufferSize, maxBufferSize);
        }

        public KryoObjectOutputStream(byte[] buffer) {
            super(buffer);
        }

        public KryoObjectOutputStream(byte[] buffer, int maxBufferSize) {
            super(buffer, maxBufferSize);
        }

        public KryoObjectOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        public KryoObjectOutputStream(OutputStream outputStream, int bufferSize) {
            super(outputStream, bufferSize);
        }

        @Override
        public void writeChar(int v) throws IOException {
            writeChar((char) v);
        }

        @Override
        public void writeBytes(String s) throws IOException {
            int len = s.length();
            for (int i = 0; i < len; i++) {
                write((byte) s.charAt(i));
            }
        }

        @Override
        public void writeChars(String s) throws IOException {
            int len = s.length();
            for (int i = 0; i < len; i++) {
                int v = s.charAt(i);
                write((v >>> 8) & 0xFF);
                write(v & 0xFF);
            }
        }

        @Override
        public void writeUTF(String s) throws IOException {
            writeString(s);
        }

        @Override
        public void writeObject(Object obj) throws IOException {
            kryo.writeClassAndObject(this, obj);
        }
    }

    class KryoObjectInputStream extends Input implements DataInput, ObjectInput {
        public KryoObjectInputStream() {
        }

        public KryoObjectInputStream(int bufferSize) {
            super(bufferSize);
        }

        public KryoObjectInputStream(byte[] buffer) {
            super(buffer);
        }

        public KryoObjectInputStream(byte[] buffer, int offset, int count) {
            super(buffer, offset, count);
        }

        public KryoObjectInputStream(InputStream inputStream) {
            super(inputStream);
        }

        public KryoObjectInputStream(InputStream inputStream, int bufferSize) {
            super(inputStream, bufferSize);
        }

        @Override
        public void readFully(byte[] b) throws IOException {
            readFully(b, 0, b.length);
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            if (len < 0)
                throw new IndexOutOfBoundsException();
            int n = 0;
            while (n < len) {
                int count = read(b, off + n, len - n);
                if (count < 0)
                    throw new EOFException();
                n += count;
            }
        }

        @Override
        public int skipBytes(int n) throws IOException {
            int total = 0;
            int cur = 0;

            while ((total < n) && ((cur = (int) skip((long) n - total)) > 0)) {
                total += cur;
            }

            return total;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            return readByteUnsigned();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return readShortUnsigned();
        }

        @Override
        public String readUTF() throws IOException {
            return readString();
        }

        @Override
        public String readLine() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object readObject() throws ClassNotFoundException, IOException {
            return kryo.readClassAndObject(this);
        }
    }
}
