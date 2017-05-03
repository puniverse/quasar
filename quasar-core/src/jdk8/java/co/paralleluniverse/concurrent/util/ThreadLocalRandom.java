/*
 * Copyright (c) 2013-2017, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.concurrent.util;


/**
  Drop-in replacement for ThreadLocalRandom (with different implementations for JDK8 and JDK7).
*/
public final class ThreadLocalRandom {
    public static java.util.concurrent.ThreadLocalRandom current() {
        return java.util.concurrent.ThreadLocalRandom.current();
    }
}
