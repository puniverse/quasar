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
package co.paralleluniverse.remote.galaxy;

import co.paralleluniverse.io.serialization.KryoSerializer;

/**
 *
 * @author eitan
 */
public final class RemoteInit {
    static {
//        KryoSerializer.register(GlxRemoteChannel.class);
//        KryoSerializer.register(GlxRemoteActor.class);
//        KryoSerializer.register(GlxRemoteActor.getActorLifecycleListenerClass());
//        KryoSerializer.register(GlxRemoteChannel.CloseMessage.class);
//        KryoSerializer.register(co.paralleluniverse.actors.ExitMessage.class);
//        KryoSerializer.register(co.paralleluniverse.actors.ShutdownMessage.class);
//        KryoSerializer.register(GlxRemoteChannel.RefMessage.class);
    }
    
    public static void init() {
        
    }

    private RemoteInit() {
    }
}
