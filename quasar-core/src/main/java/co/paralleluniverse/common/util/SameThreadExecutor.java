/*
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
package co.paralleluniverse.common.util;

import java.util.concurrent.Executor;

/**
 *
 * @author pron
 */
public final class SameThreadExecutor implements Executor {
    private static final SameThreadExecutor INSTANCE = new SameThreadExecutor();

    public static Executor getExecutor() {
        return INSTANCE;
    }
    
    @Override
    public void execute(Runnable command) {
        command.run();
    }
     
    private SameThreadExecutor() {
    }
}
