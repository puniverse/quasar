/*
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
package co.paralleluniverse.common.test;

import org.hamcrest.Matcher;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 *
 * @author pron
 */
public final class Matchers {

    // Comparable
    private static abstract class ComparableMatcher<T extends Comparable<T>> extends BaseMatcher<T> {

        private final T item;

        public ComparableMatcher(T item) {
            this.item = item;
        }

        @Override
        public boolean matches(Object other) {
            try {
                return matches(-item.compareTo((T) other)); // invert
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(this.toString());
        }

        protected abstract boolean matches(int result);
    }

    public static <T extends Comparable<T>> Matcher<T> lessThan(T value) {
        return new ComparableMatcher<T>(value) {
            @Override
            protected boolean matches(int result) {
                return result < 0;
            }

            @Override
            public String toString() {
                return "less than " + value;
            }
        };
    }

    public static <T extends Comparable<T>> Matcher<T> lessOrEqual(T value) {
        return new ComparableMatcher<T>(value) {
            @Override
            protected boolean matches(int result) {
                return result <= 0;
            }

            @Override
            public String toString() {
                return "less than or equal to " + value;
            }
        };
    }

    public static <T extends Comparable<T>> Matcher<T> greaterThan(T value) {
        return new ComparableMatcher<T>(value) {
            @Override
            protected boolean matches(int result) {
                return result > 0;
            }

            @Override
            public String toString() {
                return "greater than " + value;
            }
        };
    }

    public static <T extends Comparable<T>> Matcher<T> greaterOrEqual(T value) {
        return new ComparableMatcher<T>(value) {
            @Override
            protected boolean matches(int result) {
                return result >= 0;
            }

            @Override
            public String toString() {
                return "greater than or equal to " + value;
            }
        };
    }

    // Number
//    public static Matcher<Number> equalsWithDelta(Number value, Number delta) {
//        return new EqualsWithDelta(value, delta);
//    }

    
    // String
    
    private static abstract class StringMatcher extends BaseMatcher<String> {
        @Override
        public boolean matches(Object item) {
            if (!(item instanceof String))
                return false;
            return matches((String) item);
        }

        protected abstract boolean matches(String item);
    }
    
    public static Matcher<String> startsWith(final String prefix) {
        return new StringMatcher() {
            @Override
            protected boolean matches(String value) {
                return value.startsWith(prefix);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("startsWith");
            }
        };
    }

    public static Matcher<String> endsWith(final String suffix) {
        return new StringMatcher() {
            @Override
            protected boolean matches(String value) {
                return value.endsWith(suffix);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("endsWith");
            }
        };
    }

    public static Matcher<String> contains(final String string) {
       return new StringMatcher() {
            @Override
            protected boolean matches(String value) {
                return value.contains(string);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("contains");
            }
        };
    }

    public static Matcher<String> matches(final String regex) {
        return new StringMatcher() {
            @Override
            protected boolean matches(String value) {
                return value.matches(regex);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("matches");
            }
        };
    }

    // Object
    public static Matcher<Object> instanceOf(final Class clazz) {
        return new BaseMatcher<Object>() {
            @Override
            public boolean matches(Object value) {
                return clazz.isInstance(value);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("instanceOf");
            }
        };
    }

    // 
    private Matchers() {
    }
}
