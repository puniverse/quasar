package co.paralleluniverse.fibers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by fabio on 12/13/15.
 */
public class OffsetClassReader extends ClassReader {
    public OffsetClassReader(byte[] bytes) {
        super(bytes);
    }

    public OffsetClassReader(byte[] bytes, int i, int i1) {
        super(bytes, i, i1);
    }

    public OffsetClassReader(InputStream inputStream) throws IOException {
        super(inputStream);
    }

    public OffsetClassReader(String s) throws IOException {
        super(s);
    }

    @Override
    protected Label readLabel(int i, Label[] labels) {
        final Label ret = super.readLabel(i, labels);
        ret.info = i;
        return ret;
    }
}
