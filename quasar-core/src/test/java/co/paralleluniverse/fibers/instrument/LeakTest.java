/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Stack;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * See bug #282 (https://github.com/puniverse/quasar/issues/282)
 * @author pron
 */
public class LeakTest implements SuspendableRunnable {
    private static volatile String leaked = "leaked";
    @Test
    public void leaky() throws Exception {
        Fiber co = new Fiber((String)null, null, this);
        
        leaked = "leaked";
        
        TestsHelper.exec(co);
        TestsHelper.exec(co);
        TestsHelper.exec(co);
        
        Field stackField = Fiber.class.getDeclaredField("stack");
        stackField.setAccessible(true);
        Field objectsField = Stack.class.getDeclaredField("dataObject");
        objectsField.setAccessible(true);
        List<Object> stack = Arrays.asList((Object[])objectsField.get(stackField.get(co)));
        
//        System.out.println(stack);
        assertThat(stack, not(hasItem(leaked)));
//        assertThat(stack, everyItem(nullValue()));
        
//        WeakReference<String> ref = new WeakReference<>(leaked);
//        leaked = null;
//        System.gc();
//        System.gc();
//        assertNull(ref.get());
    }

    @Override
    public void run() throws SuspendExecution {
        Object a = new Object(); // this is so that we have more objects than primitives
        do {
            String leaked = LeakTest.leaked;
            Fiber.yield(); // foo(); // 
        } while(false);
        do {
            int primitive = 2;
            Fiber.yield(); // foo(); // 
        } while(false);
    }
    
    public void foo() throws SuspendExecution {
        String x = "oops!";
        long y = 42;
        Fiber.yield();
    }

}
