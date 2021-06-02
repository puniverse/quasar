package co.paralleluniverse.fibers.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;

import static co.paralleluniverse.common.asm.ASMUtil.ASMAPI;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

final class ExtractSuperClass extends ClassVisitor {
    private String superClass;

    private ExtractSuperClass() {
        super(ASMAPI);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.superClass = superName;
    }

    static String extractFrom(InputStream is) throws IOException {
        ClassReader reader = new ClassReader(is);
        ExtractSuperClass esc = new ExtractSuperClass();
        reader.accept(esc, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
        return esc.superClass;
    }
}
