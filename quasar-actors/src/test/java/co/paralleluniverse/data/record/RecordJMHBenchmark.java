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

import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class RecordJMHBenchmark {
    /*
     * See: 
     * http://psy-lob-saw.blogspot.co.il/2013/04/writing-java-micro-benchmarks-with-jmh.html
     * http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/
     */
    private static final String BENCHMARK = RecordJMHBenchmark.class.getName() + ".*";

    public static void main(String[] args) throws Exception {
        // Main.main(new String[]{"-usage"});
        Main.main(buildArguments(BENCHMARK, 5, 5000, 1));
    }

    private static String[] buildArguments(String className, int nRuns, int runForMilliseconds, int nThreads) {
        return new String[]{className,
                    "-f", "1",
                    "-i", "" + nRuns,
                    "-r", runForMilliseconds + "ms",
                    "-t", "" + nThreads,
                    "-w", "5000ms",
                    "-wi", "3",
                    "-prof", "stack,hs_rt,gc"
                };
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
    static final RecordType<Foo> rt = RecordType.newType(Foo.class);
    static final Field.IntField<Foo> $a = rt.intField("a");
    static final Field.DoubleField<Foo> $b = rt.doubleField("b");
    static final Field.LongField<Foo> $c = rt.longField("c");
    ThreadLocalRandom r;
    Foo x;
    Record<Foo> simple;
    Record<Foo> dynamicMethodHandle;
    Record<Foo> dynamicReflection;
    Record<Foo> dynamicUnsafe;
    Record<Foo> dynamicGeneration;

    @Setup(Level.Iteration)
    public void init() {
        r = ThreadLocalRandom.current();
        x = new Foo();
        simple = rt.newInstance();
        dynamicMethodHandle = rt.newInstance(new Foo1(), RecordType.Mode.METHOD_HANDLE);
        dynamicReflection = rt.newInstance(new Foo2(), RecordType.Mode.REFLECTION);
        dynamicUnsafe = rt.newInstance(new Foo3(), RecordType.Mode.UNSAFE);
        dynamicGeneration = rt.newInstance(new Foo4(), RecordType.Mode.GENERATION);
    }

    @GenerateMicroBenchmark
    public Object timeSimple() {
        return foo(simple);
    }

    @GenerateMicroBenchmark
    public Object timeDynamicMethodHandle() {
        return foo(dynamicMethodHandle);
    }

    @GenerateMicroBenchmark
    public Object timeDynamicReflection() {
        return foo(dynamicReflection);
    }

    @GenerateMicroBenchmark
    public Object timeDynamicUnsafe() {
        return foo(dynamicUnsafe);
    }

    @GenerateMicroBenchmark
    public Object timeDynamicGeneration() {
        return foo(dynamicGeneration);
    }

    private Object foo(Record<Foo> x) {
        x.set($a, (int) x.get($c) + r.nextInt(100));
        x.set($b, x.get($a) + r.nextDouble(100));
        x.set($c, (long) x.get($b) + r.nextLong(200));

        return x;
    }

    @GenerateMicroBenchmark
    public Object timePojo() {
        x.a = (int) x.c + r.nextInt(100);
        x.b = x.a + r.nextDouble(100);
        x.c = (long) x.b + r.nextLong(200);

        return x;
    }
}
