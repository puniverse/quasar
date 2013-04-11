/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import co.paralleluniverse.fibers.instrument.MethodDatabase.ClassEntry;
import java.util.ServiceLoader;

/**
 *
 * @author pron
 */
class SuspendableClassifierService {
    private static ServiceLoader<SuspendableClassifier> loader = ServiceLoader.load(SuspendableClassifier.class);

    public static boolean isSuspendable(String className, ClassEntry classEntry, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        for (SuspendableClassifier sc : loader) {
            if (sc.isSuspendable(className, classEntry.superName, classEntry.getInterfaces(), methodName, methodDesc, methodSignature, methodExceptions))
                return true;
        }
        if (checkExceptions(methodExceptions))
            return true;
        return false;
    }

    private static boolean checkExceptions(String[] exceptions) {
        if (exceptions != null) {
            for (String ex : exceptions) {
                if (ex.equals(EXCEPTION_NAME))
                    return true;
            }
        }
        return false;
    }
}
