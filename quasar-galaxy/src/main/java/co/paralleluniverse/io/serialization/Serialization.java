/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.io.serialization;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.locks.ReentrantLock;
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
