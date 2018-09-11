package co.paralleluniverse.fibers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Christian Sailer (christian.sailer@r3.com)
 */
public class CustomFiberWriterSerializer extends Serializer<CustomFiberWriter> {
    @Override
    public void write(Kryo kryo, Output output, CustomFiberWriter object) {
    }

    @Override
    public CustomFiberWriter read(Kryo kryo, Input input, Class<CustomFiberWriter> type) {
        return null;
    }
}
