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
package co.paralleluniverse.common.test;

import org.hamcrest.Matcher;
import org.mockito.internal.matchers.Contains;
import org.mockito.internal.matchers.EndsWith;
import org.mockito.internal.matchers.EqualsWithDelta;
import org.mockito.internal.matchers.Find;
import org.mockito.internal.matchers.GreaterOrEqual;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.internal.matchers.InstanceOf;
import org.mockito.internal.matchers.LessOrEqual;
import org.mockito.internal.matchers.LessThan;
import org.mockito.internal.matchers.Matches;
import org.mockito.internal.matchers.StartsWith;

/**
 *
 * @author pron
 */
public final class Matchers {

    // Comparable

    public static <T extends Comparable<T>> Matcher<T> lessThan(Comparable<T> value) {
        return new LessThan<T>(value);
    }

    public static <T extends Comparable<T>> Matcher<T> lessOrEqual(Comparable<T> value) {
        return new LessOrEqual<T>(value);
    }

    public static <T extends Comparable<T>> Matcher<T> greaterThan(Comparable<T> value) {
        return new GreaterThan<T>(value);
    }

    public static <T extends Comparable<T>> Matcher<T> greaterOrEqual(Comparable<T> value) {
        return new GreaterOrEqual<T>(value);
    }

    // Number
    public static Matcher<Number> equalsWithDelta(Number value, Number delta) {
        return new EqualsWithDelta(value, delta);
    }
    
    // String

    public static Matcher<String> startsWith(String prefix) {
        return new StartsWith(prefix);
    }

    public static Matcher<String> endsWith(String suffix) {
        return new EndsWith(suffix);
    }

    public static Matcher<String> contains(String string) {
        return new Contains(string);
    }

    public static Matcher<String> matches(String regex) {
        return (Matcher<String>)((Object)new Matches(regex));
    }

    public static Matcher<String> find(String regex) {
        return new Find(regex);
    }

    // Object

    public static Matcher<Object> instanceOf(Class clazz) {
        return new InstanceOf(clazz);
    }

    // 
    private Matchers() {
    }
}
