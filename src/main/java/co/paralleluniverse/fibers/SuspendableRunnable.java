package co.paralleluniverse.fibers;

public interface SuspendableRunnable {
    void run() throws SuspendExecution, InterruptedException;
}
