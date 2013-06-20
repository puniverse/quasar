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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.shaded.org.objenesis.strategy.SerializingInstantiatorStrategy;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
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

/**
 *
 * @author pron
 */
public class KryoSerializer {
    public static final Kryo KRYO;

    static {
        KRYO = new KryoReflectionFactorySupport();

        KRYO.setRegistrationRequired(false);
        KRYO.setInstantiatorStrategy(new SerializingInstantiatorStrategy());

        KRYO.register(boolean[].class);
        KRYO.register(byte[].class);
        KRYO.register(short[].class);
        KRYO.register(char[].class);
        KRYO.register(int[].class);
        KRYO.register(float[].class);
        KRYO.register(long[].class);
        KRYO.register(double[].class);
        KRYO.register(String[].class);
        KRYO.register(int[][].class);
        KRYO.register(java.util.ArrayList.class);
        KRYO.register(java.util.LinkedList.class);
        KRYO.register(java.util.HashMap.class);
        KRYO.register(java.util.LinkedHashMap.class);
        KRYO.register(java.util.TreeMap.class);
        KRYO.register(java.util.EnumMap.class);
        KRYO.register(java.util.HashSet.class);
        KRYO.register(java.util.TreeSet.class);
        KRYO.register(java.util.EnumSet.class);

        KRYO.register(java.util.Arrays.asList("").getClass(), new ArraysAsListSerializer());
//        KRYO.register(java.util.Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
//        KRYO.register(java.util.Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
//        KRYO.register(java.util.Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
//        KRYO.register(java.util.Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
//        KRYO.register(java.util.Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
//        KRYO.register(java.util.Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
        KRYO.register(java.util.GregorianCalendar.class, new GregorianCalendarSerializer());
        KRYO.register(java.lang.reflect.InvocationHandler.class, new JdkProxySerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(KRYO);
        SynchronizedCollectionsSerializer.registerSerializers(KRYO);

        KRYO.addDefaultSerializer(Externalizable.class, new ExternalizableSerializer());
    }

    public static <T> T readKryo(InputStream is, Class<T> type) {
        final Input input = new KryoObjectInputStream(is);
        return KRYO.readObjectOrNull(input, type);
    }

    public static <T> T readKryo(byte[] buffer, Class<T> type) {
        final Input input = new KryoObjectInputStream(buffer);
        return KRYO.readObjectOrNull(input, type);
    }

    public static void writeKryo(OutputStream os, Object object) {
        final Output output = new KryoObjectOutputStream(os);
        KRYO.writeClassAndObject(output, object);
        output.flush();
    }

    public static byte[] writeKryo(Object object) {
        final Output output = new KryoObjectOutputStream(512, -1);
        KRYO.writeClassAndObject(output, object);
        output.flush();
        return output.toBytes();
    }

    private KryoSerializer() {
    }

    static class KryoObjectOutputStream extends Output implements DataOutput, ObjectOutput {

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
            KRYO.writeClassAndObject(this, obj);
        }
    }

    static class KryoObjectInputStream extends Input implements DataInput, ObjectInput {
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
            return KRYO.readClassAndObject(this);
        }
    }
}
