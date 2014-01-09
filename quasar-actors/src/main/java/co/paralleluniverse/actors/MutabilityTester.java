/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.actors;

//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import org.mutabilitydetector.AnalysisResult;
//import org.mutabilitydetector.AnalysisSession;
//import org.mutabilitydetector.Configuration;
//import org.mutabilitydetector.ConfigurationBuilder;
//import org.mutabilitydetector.Configurations;
//import org.mutabilitydetector.IsImmutable;
//import org.mutabilitydetector.MutabilityReason;
//import org.mutabilitydetector.MutableReasonDetail;
//import org.mutabilitydetector.ThreadUnsafeAnalysisSession;
//import org.mutabilitydetector.locations.Dotted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class MutabilityTester {
    private static final Logger LOG = LoggerFactory.getLogger(MutabilityTester.class);
    private static final boolean warn = Boolean.getBoolean("co.paralleluniverse.warnOnImmutable");
//    private static final Configuration ACTOR_CONFIGURATION = new ConfigurationBuilder() {
//        @Override
//        public void configure() {
//            mergeHardcodedResultsFrom(Configurations.JDK_CONFIGURATION);
//            hardcodeAsDefinitelyImmutable(ActorRef.class);
//        }
//    }.build();
//
//    private static final ClassValue<AnalysisResult> classAnalysisResults = new ClassValue<AnalysisResult>() {
//        @Override
//        protected AnalysisResult computeValue(Class<?> type) {
//            if (warn) {
//                final AnalysisSession analysisSession = ThreadUnsafeAnalysisSession.createWithCurrentClassPath(ACTOR_CONFIGURATION);
//                AnalysisResult result = analysisSession.resultFor(Dotted.fromClass(type));
//                List<MutableReasonDetail> reasons = new ArrayList<>(result.reasons);
//                for (Iterator<MutableReasonDetail> it = reasons.iterator(); it.hasNext();) {
//                    if (it.next().reason() == MutabilityReason.CAN_BE_SUBCLASSED)
//                        it.remove();
//                }
//                if (result.isImmutable == IsImmutable.NOT_IMMUTABLE && !reasons.isEmpty()) {
//                    LOG.warn("Class " + type.getName() + " is not immutable; reasons: " + reasons, new MutableWarning(type));
//                }
//                return result;
//            } else
//                return null;
//        }
//    };

    public static void testMutability(Class<?> clazz) {
//        if (warn)
//            classAnalysisResults.get(clazz);
    }

    public static void testMutability(Object obj) {
//        testMutability(obj.getClass());
    }

    private static class MutableWarning extends Exception {
        public MutableWarning(Class<?> cls) {
            super("Class " + cls.getName() + " is mutable");
        }
    }
}
