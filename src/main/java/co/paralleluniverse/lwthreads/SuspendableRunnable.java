package co.paralleluniverse.lwthreads;

/**
 * A class that implements this interface can be run as a Coroutine.
 * 
 * @see Coroutine
 * @author Matthias Mann
 */
public interface SuspendableRunnable {

    /**
     * Entry point for Coroutine execution.
     * 
     * This method should never be called directly.
     * 
     * @see Coroutine#Coroutine(de.matthiasmann.continuations.CoroutineProto) 
     * @see Coroutine#run()
     * @see SuspendExecution
     * @throws de.matthiasmann.continuations.SuspendExecution This exception should never be cought
     */
    public void run() throws SuspendExecution;
    
}
