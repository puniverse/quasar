package co.paralleluniverse.kotlin;

import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;

/**
 * Created by fabio on 3/8/15.
 */
public class KotlinSuspendableClassifier implements SuspendableClassifier {
    private final KotlinInstrumentListProvider kotlinInstrumentListProvider = new KotlinInstrumentListProvider();

    @Override
    public MethodDatabase.SuspendableType isSuspendable(final MethodDatabase db, final String sourceName, final String sourceDebugInfo,
                                                        final boolean isInterface, final String className, final String superClassName,
                                                        final String[] interfaces, final String methodName, final String methodDesc,
                                                        final String methodSignature, final String[] methodExceptions) {
        final InstrumentMatcher.Match<MethodDatabase.SuspendableType> t =
            match(db, sourceName, sourceDebugInfo, isInterface, className, superClassName, interfaces,
                methodName, methodDesc, methodSignature, methodExceptions);
        if (t != null)
            return t.getValue();

        KotlinInstrumentListProvider.log(db, "kotlin", "evaluation of matchlist didn't say anything",
            sourceName, isInterface, className, superClassName, interfaces, methodName, methodSignature);

        return null;
    }

    public InstrumentMatcher.Match<MethodDatabase.SuspendableType> match(final MethodDatabase db, final String sourceName, final String sourceDebugInfo,
                                                                                final boolean isInterface, final String className, final String superClassName, final String[] interfaces,
                                                                                final String methodName, final String methodDesc, final String methodSignature, final String[] methodExceptions) {
        for (final InstrumentMatcher m : kotlinInstrumentListProvider.getMatchList()) {
            final InstrumentMatcher.Match<MethodDatabase.SuspendableType> t =
                m.eval(db, sourceName, sourceDebugInfo, isInterface, className, superClassName, interfaces,
                    methodName, methodDesc, methodSignature, methodExceptions);
            if (t != null)
                return t;
        }
        return null;
    }
}
