package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.SuspendExecution;

public interface SuspendableRunnable {
    void run() throws SuspendExecution, InterruptedException;
}
