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

    private static class Simple {
        static final SimpleRecordType<Simple> rt = new SimpleRecordType<>();
        static final Field.IntField<Simple> $a = rt.intField("a");
        static final Field.DoubleField<Simple> $b = rt.doubleField("b");
        static final Field.LongField<Simple> $c = rt.longField("c");
    }

    private static class Dynamic {
        final DynamicRecordType<Foo> rt;
        final Field.IntField<Foo> $a;
        final Field.DoubleField<Foo> $b;
        final Field.LongField<Foo> $c;

        Dynamic(DynamicRecordType.Mode mode) {
            rt = new DynamicRecordType<>(mode);
            $a = rt.intField("a");
            $b = rt.doubleField("b");
            $c = rt.longField("c");
        }
    }

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
        final ThreadLocalRandom r = ThreadLocalRandom.current();

        Record<Simple> x = Simple.rt.newInstance();
        for (int i = 0; i < reps; i++) {
            x.set(Simple.$a, (int) x.get(Simple.$c) + r.nextInt(100));
            x.set(Simple.$b, x.get(Simple.$a) + r.nextDouble(100));
            x.set(Simple.$c, (long) x.get(Simple.$b) + r.nextLong(200));
        }
        return x;
    }

    public Object timeDynamicMethodHandle(int reps) {
        return dyn(DynamicRecordType.Mode.METHOD_HANDLE, reps);
    }

    public Object timeDynamicReflection(int reps) {
        return dyn(DynamicRecordType.Mode.REFLECTION, reps);
    }
    
    public Object timeDynamicUnsafe(int reps) {
        return dyn(DynamicRecordType.Mode.UNSAFE, reps);
    }
    
    public Object timeDynamicGeneration(int reps) {
        return dyn(DynamicRecordType.Mode.GENERATION, reps);
    }
    
    private Object dyn(DynamicRecordType.Mode mode, int reps) {
        Dynamic dynamic = new Dynamic(mode);
        final ThreadLocalRandom r = ThreadLocalRandom.current();

        Record<Foo> x = dynamic.rt.newInstance(new Foo());
        for (int i = 0; i < reps; i++) {
            x.set(dynamic.$a, (int) x.get(dynamic.$c) + r.nextInt(100));
            x.set(dynamic.$b, x.get(dynamic.$a) + r.nextDouble(100));
            x.set(dynamic.$c, (long) x.get(dynamic.$b) + r.nextLong(200));
        }
        return x;
    }
}
