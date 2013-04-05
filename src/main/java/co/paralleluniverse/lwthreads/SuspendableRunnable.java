package co.paralleluniverse.lwthreads;

public interface SuspendableRunnable {
    void run() throws SuspendExecution;
}
