/*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test to checking blocking call detection
 *
 * @author Matthias Mann
 */
@Instrumented
public class BlockingTest {

    @Test
    public void testSuspend() throws IOException {
        final String className = BlockingTest.class.getName().replace('.', '/');
        final HashSet<String> msgs = new HashSet<String>();
        msgs.add("Method " + className + "#t_wait()V contains potentially blocking call to java/lang/Object#wait()V");
        msgs.add("Method " + className + "#t_sleep1()V contains potentially blocking call to java/lang/Thread#sleep(J)V");
        msgs.add("Method " + className + "#t_sleep2()V contains potentially blocking call to java/lang/Thread#sleep(JI)V");
        msgs.add("Method " + className + "#t_join1(Ljava/lang/Thread;)V contains potentially blocking call to java/lang/Thread#join()V");
        msgs.add("Method " + className + "#t_join2(Ljava/lang/Thread;)V contains potentially blocking call to java/lang/Thread#join(J)V");
        msgs.add("Method " + className + "#t_join3(Ljava/lang/Thread;)V contains potentially blocking call to java/lang/Thread#join(JI)V");

        final QuasarInstrumentor instrumentor = new QuasarInstrumentor(BlockingTest.class.getClassLoader());
        final MethodDatabase db = instrumentor.getMethodDatabase();
        db.setAllowBlocking(true);
        db.setLog(new Log() {
            public void log(LogLevel level, String msg, Object... args) {
                if (level == LogLevel.WARNING) {
                    msg = String.format(Locale.ENGLISH, msg, args);
                    assertTrue("Unexpected message: " + msg, msgs.remove(msg));
                }
            }

            public void error(String msg, Throwable ex) {
                throw new Error(msg, ex);
            }
        });

        try (InputStream in = BlockingTest.class.getResourceAsStream("BlockingTest.class")) {
            instrumentor.instrumentClass(BlockingTest.class.getName(), in, true);
        }

        assertTrue("Expected messages not generated: " + msgs.toString(), msgs.isEmpty());
    }

    public void t_wait() throws SuspendExecution, InterruptedException {
        synchronized (this) {
            wait();
        }
    }

    public void t_notify() throws SuspendExecution {
        synchronized (this) {
            notify();
        }
    }

    public void t_sleep1() throws SuspendExecution, InterruptedException {
        Thread.sleep(1000);
    }

    public void t_sleep2() throws SuspendExecution, InterruptedException {
        Thread.sleep(1000, 100);
    }

    public void t_join1(Thread t) throws SuspendExecution, InterruptedException {
        t.join();
    }

    public void t_join2(Thread t) throws SuspendExecution, InterruptedException {
        t.join(1000);
    }

    public void t_join3(Thread t) throws SuspendExecution, InterruptedException {
        t.join(1, 100);
    }

    public void t_lock3() throws SuspendExecution {
        lock();
    }

    public void lock() {
        System.out.println("Just a method which have similar signature");
    }
}
