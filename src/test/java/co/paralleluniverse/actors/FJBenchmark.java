package co.paralleluniverse.actors;

import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinTask;
import jsr166e.RecursiveAction;


public class FJBenchmark {
    static final int PARALLELISM = 4;
    static final int COUNT = 1000000;
    static ForkJoinPool fjPool = new ForkJoinPool(PARALLELISM, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++)
            run(COUNT);
    }

    static void run(int count) throws Exception {
        RecursiveAction lastTask = new RecursiveAction() {
            @Override
            protected void compute() {
            }
        };
        final long start = System.nanoTime();
        fjPool.submit(new MyTask(count, lastTask));
        lastTask.get();
        System.out.println("count: " + count + " time: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
    }

    static class MyTask extends RecursiveAction {
        final int count;
        final ForkJoinTask lastTask;

        public MyTask(int count, ForkJoinTask lastTask) {
            this.count = count;
            this.lastTask = lastTask;
        }

        @Override
        protected void compute() {
            if (count > 0)
                new MyTask(count - 1, lastTask).fork();
            else
                lastTask.fork();
        }
    }
}
