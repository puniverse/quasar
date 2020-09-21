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

package co.paralleluniverse.fibers;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author pron
 */
class FiberSerializer {
    /**
     * This method returns once the fiber has been deserialized
     * @param os
     * @throws SuspendExecution
     * @throws InterruptedException 
     */
    @SuppressWarnings("empty-statement")
    public void serialize(OutputStream os) throws SuspendExecution, InterruptedException {
        final Fiber<?> fiber = Fiber.currentFiber();
        fiber.record(1, "FiberAsync", "run", "Blocking fiber %s on FibeAsync %s", fiber, this);
        while (!Fiber.park(this, new Fiber.ParkAction() {
            @Override
            public void run(Fiber<?> current) {
                try {
                    // serialize
                } catch (Throwable t) {
                    //
                }
            }
        })); 
    }
    
    /**
     * Deserializes and restarts a serialized fiber.
     * @param is
     * @param scheduler 
     */
    public void deserialize(InputStream is, FiberScheduler scheduler) {
        
    }
}
