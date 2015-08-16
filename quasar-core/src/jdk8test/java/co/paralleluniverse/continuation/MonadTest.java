/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.continuation;

import static co.paralleluniverse.continuation.ExceptionC.*;
import static co.paralleluniverse.continuation.ReaderC.*;
import static co.paralleluniverse.continuation.WriterC.*;
import static co.paralleluniverse.continuation.StateC.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonadTest {

    static class MyException extends ExceptionScope {
        public MyException(String message) {
            super(message);
        }
    };

    @Test
    public void testException() {

        int res
                = tryc(() -> {
                    throwc(new MyException("12345"));
                    return 3;
                },
                MyException.class,
                ex -> {
                    return ex.message.length();
                });
        assertEquals(5, res);
    }

    // It would have been awesome if Java allowed generic exception classes so we could have ReaderScope<String> or ReaderScope<Integer>
    static class Reader1 extends ReaderScope {
    }

    static class Reader2 extends ReaderScope {
    }

    @Test
    public void testReader() {
        int res = runReader(Reader1.class, 5, () -> {
            return runReader(Reader2.class, "hello", () -> {

                int a = get(Reader1.class);
                String b = get(Reader2.class);

                return a + b.length();
            });
        });
        assertEquals(10, res);
    }

    // It would have been awesome if Java allowed generic exception classes so we could have WriterScope<String> or WriterScope<Integer>
    static class Writer1 extends WriterScope {
    }

    static class Writer2 extends WriterScope {
    }

    @Test
    public void testWriter() {
        WriterC<Writer1, String> writer1 = new WriterC<>(Writer1.class);
        WriterC<Writer2, Integer> writer2 = new WriterC<>(Writer2.class);

        int res = writer1.runWriter(() -> {
            return writer2.runWriter(() -> {

                add(Writer1.class, "a");
                add(Writer2.class, 1);
                add(Writer1.class, "b");
                add(Writer2.class, 2);
                add(Writer1.class, "c");
                add(Writer2.class, 3);

                return 3;
            });
        });

        assertEquals(3, res);
        assertEquals(Arrays.asList("a", "b", "c"), writer1.getList());
        assertEquals(Arrays.asList(1, 2, 3), writer2.getList());
    }

    // It would have been awesome if Java allowed generic exception classes so we could have StateScope<String> or StateScope<Integer>
    static class State1 extends StateScope {
    }

    static class State2 extends StateScope {
    }

    @Test
    public void testState() {
        int res = runState(State1.class, 5, () -> {
            return runState(State2.class, "hello", () -> {

                int a = get(State1.class);
                String b = get(State2.class);

                set(State1.class, 3);
                set(State2.class, "byebye");

                int c = get(State1.class);
                String d = get(State2.class);

                return a + b.length() + c + d.length();
            });
        });
        assertEquals(19, res);
    }

    @Test
    public void testAllTogetherNow() {
        WriterC<Writer2, Integer> writer = new WriterC<>(Writer2.class);

        String res
                = tryc(() -> {
                    return runReader(Reader1.class, 5, () -> {
                        return runState(State2.class, "hello", () -> {
                            return writer.runWriter(() -> {

                                String b = get(State2.class);
                                assertEquals("hello", b);
                                add(Writer2.class, 13);

                                set(State2.class, b + get(Reader1.class));
                                add(Writer2.class, 100);

                                throwc(new MyException(get(State2.class)));

                                add(Writer2.class, 200);
                                fail();
                                return ""; // for the compiler

                            });
                        });
                    });
                },
                MyException.class,
                ex -> {
                    return ex.message;
                });

        assertEquals("hello5", res);
        assertEquals(Arrays.asList(13, 100), writer.getList());
    }

    @Test
    public void testAllTogetherNowWithFiber() throws Exception {
        WriterC<Writer2, Integer> writer = new WriterC<>(Writer2.class);

        String res
                = new Fiber<String>(() -> {
                    return tryc(() -> {
                        return runReader(Reader1.class, 5, () -> {
                            return runState(State2.class, "hello", () -> {
                                return writer.runWriter(() -> {
                                    try {
                                        String b = get(State2.class);
                                        assertEquals("hello", b);

                                        add(Writer2.class, 13);

                                        Fiber.sleep(50);

                                        set(State2.class, b + get(Reader1.class));

                                        Fiber.sleep(50);

                                        add(Writer2.class, 100);

                                        Fiber.sleep(50);

                                        throwc(new MyException(get(State2.class)));

                                        add(Writer2.class, 200);

                                        fail();
                                        return ""; // for the compiler
                                    } catch (Exception e) {
                                        throw new AssertionError(e);
                                    }
                                });
                            });
                        });
                    },
                    MyException.class,
                    ex -> {
                        return ex.message;
                    });
                }).start().get();

        assertEquals("hello5", res);
        assertEquals(Arrays.asList(13, 100), writer.getList());
    }

}
