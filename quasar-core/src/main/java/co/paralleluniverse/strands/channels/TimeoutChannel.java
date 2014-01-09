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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.concurrent.util.ScheduledSingleThreadExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class TimeoutChannel<Message> extends TransferChannel<Message> {
    public static <Message> Channel<Message> timeout(long timeout, TimeUnit unit) {
        return new TimeoutChannel<Message>(timeout, unit);
    }

    private TimeoutChannel(long timeout, TimeUnit unit) {
        timeoutService.schedule(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, timeout, unit);
    }
    private static final ScheduledExecutorService timeoutService = new ScheduledSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("TimeoutChannelCloser-%d").setDaemon(true).build());

//    static {
//
//        try {
//            Field f = Fiber.class.getDeclaredField("timeoutService");
//            f.setAccessible(true);
//            timeoutService = (ScheduledExecutorService) f.get(null);
//        } catch (Exception e) {
//            throw new AssertionError(e);
//        }
//    }
}
