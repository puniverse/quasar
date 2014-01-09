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
import com.esotericsoftware.kryo.io.Input;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

/**
 *
 * @author pron
 */
class KryoObjectInputStream extends InputStream implements DataInput, ObjectInput {
    private final Input in;
    private final Kryo kryo;

    public KryoObjectInputStream(Input in, Kryo kryo) {
        this.in = in;
        this.kryo = kryo;
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
        while ((total < n) && ((cur = (int) skip((long) n - total)) > 0))
            total += cur;
        return total;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return in.readByteUnsigned();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return in.readShortUnsigned();
    }

    @Override
    public String readUTF() throws IOException {
        return in.readString();
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        return kryo.readClassAndObject(in);
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    @Override
    public short readShort() throws IOException {
        return in.readShort();
    }

    @Override
    public char readChar() throws IOException {
        return in.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return in.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return in.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
    
}
