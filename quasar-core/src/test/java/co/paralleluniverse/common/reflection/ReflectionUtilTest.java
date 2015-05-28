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
package co.paralleluniverse.common.reflection;

import co.paralleluniverse.common.test.TestUtil;
import java.lang.reflect.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 *
 * @author pron
 */
public class ReflectionUtilTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    static class A<T> {

    }

    static class B<X, T> extends A<T> {

    }

    static class C extends B<Integer, String> {

    }

    static class D extends A<String> {

    }

    static class E extends A<String> {

    }

    static interface IA<T> {

    }

    static interface IB<X, T> extends IA<T> {

    }

    static interface IC extends IB<Integer, String> {

    }

    static interface ID extends IA<String> {

    }

    static interface IE extends IA<String> {

    }

    @Test
    public void testGetGenericParameter1() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(D.class, A.class, 0);
        assertEquals(res, String.class);
    }

    @Test
    public void testGetGenericParameter2() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(new A<String>() {
        }.getClass(), A.class, 0);
        assertEquals(res, String.class);
    }

    @Test
    public void testGetGenericParameter3() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(E.class, A.class, 0);
        assertEquals(res, String.class);
    }

    @Ignore
    @Test
    public void testGetGenericParameter4() {
        Type res = ReflectionUtil.getGenericParameterType(new B<Integer, String>().getClass(), A.class, 0);
        assertEquals(res, String.class);
    }

    @Test
    public void testGetGenericParameterInterface1() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(ID.class, IA.class, 0);
        assertEquals(res, String.class);
    }

    @Test
    public void testGetGenericParameterInterface2() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(new IA<String>() {
        }.getClass(), IA.class, 0);
        assertEquals(res, String.class);
    }

    @Test
    public void testGetGenericParameterInterface3() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(IE.class, IA.class, 0);
        assertEquals(res, String.class);
    }

    @Ignore
    @Test
    public void testGetGenericParameterInterface4() {
        Class<?> res = (Class<?>) ReflectionUtil.getGenericParameterType(new IB<Integer, String>() {
        }.getClass(), IA.class, 0);
        assertEquals(res, String.class);
    }
}
