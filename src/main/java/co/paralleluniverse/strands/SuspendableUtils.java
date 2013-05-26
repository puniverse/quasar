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
package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.SuspendExecution;

/**
 *
 * @author pron
 */
public class SuspendableUtils {
    public static SuspendableCallable<Void> runnableToCallable(SuspendableRunnable runnable) {
        return new VoidSuspendableCallable(runnable);
    }

    public static class VoidSuspendableCallable implements SuspendableCallable<Void> {
        private final SuspendableRunnable runnable;

        public VoidSuspendableCallable(SuspendableRunnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Void run() throws SuspendExecution, InterruptedException {
            runnable.run();
            return null;
        }

        public SuspendableRunnable getRunnable() {
            return runnable;
        }
    }
}
