/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    private static final ByteArraySerializer bas = useJDKSerialization ? new JDKSerializer() : new KryoSerializer();
    private static final IOStreamSerializer ioss = (IOStreamSerializer)bas;

    public static Object read(byte[] buf) {
        return bas.read(buf);
    }

    public static byte[] write(Object object) {
        return bas.write(object);
    }

    public static Object read(InputStream is) throws IOException {
        return ioss.read(is);
    }

    public static void write(OutputStream os, Object object) throws IOException {
        ioss.write(os, object);
    }
    
    
    private Serialization() {
    }
}
