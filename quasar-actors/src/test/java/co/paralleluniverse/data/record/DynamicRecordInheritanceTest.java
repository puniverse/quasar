/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.data.record;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.paralleluniverse.data.record.Field.*;
import java.util.Random;

/**
 *
 * @author pron
 */
//@RunWith(org.mockito.runners.MockitoJUnitRunner.class) //VerboseMockitoJUnitRunner.class)
public class DynamicRecordInheritanceTest {
    private static final Random rand = new Random();

    public DynamicRecordInheritanceTest() {
    }

    @Before
    public void setUp() {
    }

    private static class A {
        public boolean a;
        public int d;
        public String str;
        public double[] ga = new double[2];
        public String[] stra = new String[2];
    }

    private static class B extends A {
        public int d1;
        public String str1;
    }

    private static class AR {
        static final RecordType<A> rt = RecordType.newType(A.class);
        static final BooleanField<A> $a = rt.booleanField("a");
        static final IntField<A> $d = rt.intField("d");
        static final ObjectField<A, String> $str = rt.objectField("str", String.class);
        static final DoubleArrayField<A> $ga = rt.doubleArrayField("ga", 2);
        static final ObjectArrayField<A, String> $stra = rt.objectArrayField("stra", String.class, 2);
    }

    private static class BR extends AR {
        static final RecordType<B> rt = RecordType.newType(B.class, AR.rt);
        static final IntField<B> $d1 = rt.intField("d1");
        static final ObjectField<B, String> $str1 = rt.objectField("str1", String.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test1() {
        B b1 = new B();
        Record<B> b = BR.rt.newInstance(b1);
        b.set(BR.$a, true);
        b.set(AR.$d, 15);
        b.set(BR.$str, "hi");
        b.set(AR.$ga, 0, 0.1);
        b.set(BR.$ga, 1, 0.2);
        b.set(AR.$stra, 0, "a1");
        b.set(BR.$stra, 1, "a2");
        b.set(BR.$d1, 42);
        b.set(BR.$str1, "bye");

        assertThat(b.get(AR.$a), equalTo(true));
        assertThat(b.get(BR.$d), equalTo(15));
        assertThat(b.get(AR.$str), equalTo("hi"));
        assertThat(b.get(BR.$ga, 0), equalTo(0.1));
        assertThat(b.get(AR.$ga, 1), equalTo(0.2));
        assertThat(b.get(BR.$stra, 0), equalTo("a1"));
        assertThat(b.get(AR.$stra, 1), equalTo("a2"));
        assertThat(b.get(BR.$d1), equalTo(42));
        assertThat(b.get(BR.$str1), equalTo("bye"));

        assertThat(b1.a, equalTo(true));
        assertThat(b1.d, equalTo(15));
        assertThat(b1.str, equalTo("hi"));
        assertThat(b1.ga[0], equalTo(0.1));
        assertThat(b1.ga[1], equalTo(0.2));
        assertThat(b1.stra[0], equalTo("a1"));
        assertThat(b1.stra[1], equalTo("a2"));
        assertThat(b1.d1, equalTo(42));
        assertThat(b1.str1, equalTo("bye"));
    }
}
