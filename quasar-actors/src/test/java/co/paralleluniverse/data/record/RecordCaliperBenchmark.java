/*
 * Copyright 2012 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.paralleluniverse.data.record;

import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Martin Thompson
 * @autor pron
 */
public class RecordCaliperBenchmark extends Benchmark {
    public static void main(String[] args) throws Exception {
        CaliperMain.main(RecordCaliperBenchmark.class, args);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public static class Foo {
        public int a;
        public double b;
        public long c;
    }

    public static class Foo1 extends Foo {
    }

    public static class Foo2 extends Foo {
    }

    public static class Foo3 extends Foo {
    }

    public static class Foo4 extends Foo {
    }
    static final RecordType<Foo> rt = new RecordType<>();
    static final Field.IntField<Foo> $a = rt.intField("a");
    static final Field.DoubleField<Foo> $b = rt.doubleField("b");
    static final Field.LongField<Foo> $c = rt.longField("c");

    public Object timePojo(int reps) {
        final ThreadLocalRandom r = ThreadLocalRandom.current();

        Foo x = new Foo();
        for (int i = 0; i < reps; i++) {
            x.a = (int) x.c + r.nextInt(100);
            x.b = x.a + r.nextDouble(100);
            x.c = (long) x.b + r.nextLong(200);
        }
        return x;
    }

    public Object timeSimple(int reps) {
        return foo(rt.newInstance(), reps);
    }

    public Object timeDynamicMethodHandle(int reps) {
        return foo(rt.newInstance(new Foo1(), RecordType.Mode.METHOD_HANDLE), reps);
    }

    public Object timeDynamicReflection(int reps) {
        return foo(rt.newInstance(new Foo2(), RecordType.Mode.REFLECTION), reps);
    }

    public Object timeDynamicUnsafe(int reps) {
        return foo(rt.newInstance(new Foo3(), RecordType.Mode.UNSAFE), reps);
    }

    public Object timeDynamicGeneration(int reps) {
        return foo(rt.newInstance(new Foo4(), RecordType.Mode.GENERATION), reps);
    }

    private Object foo(Record<Foo> x, int reps) {
        final ThreadLocalRandom r = ThreadLocalRandom.current();

        for (int i = 0; i < reps; i++) {
            x.set($a, (int) x.get($c) + r.nextInt(100));
            x.set($b, x.get($a) + r.nextDouble(100));
            x.set($c, (long) x.get($b) + r.nextLong(200));
        }
        return x;
    }
}
