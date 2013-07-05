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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.Fiber;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class TimeoutChannel<Message> extends SelectableTransferChannel<Message> {
    public TimeoutChannel(long timeout, TimeUnit unit) {
        fiberTimeoutService.schedule(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, timeout, unit);
    }

    private static final ScheduledExecutorService fiberTimeoutService;

    static {

        try {
            Field f = Fiber.class.getDeclaredField("timeoutService");
            f.setAccessible(true);
            fiberTimeoutService = (ScheduledExecutorService) f.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
