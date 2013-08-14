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
package co.paralleluniverse.actors;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author pron
 */
public final class ActorUtil {
    public static Object randtag() {
        return new BigInteger(80, ThreadLocalRandom.current()) {
            @Override
            public String toString() {
                return toString(16);
            }
        };
    }

    public static void sendOrInterrupt(ActorRef actor, Object message) {
        ((ActorRefImpl) actor).sendOrInterrupt(message);
    }

    private ActorUtil() {
    }
}
