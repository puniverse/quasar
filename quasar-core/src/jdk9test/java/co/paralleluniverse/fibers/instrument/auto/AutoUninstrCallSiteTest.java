package co.paralleluniverse.fibers.instrument.auto;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;

import java.util.concurrent.ExecutionException;

class F implements SuspendableRunnable {
    @Override
    public void run() throws SuspendExecution, InterruptedException {
        System.err.println("Enter run()");
        m();
        System.err.println("Exit run()");
    }

    // @Suspendable
    public void m() {
        System.err.println("Enter m()");
        m1();
        System.err.println("Exit m()");
    }

    @Suspendable
    public void m1() {
        System.err.println("Enter m1(), sleeping...");
        try {
            Fiber.sleep(1000);
        } catch (final InterruptedException | SuspendExecution e) {
            throw new RuntimeException(e);
        }
        System.err.println("...Exit m1()");
    }
}

public class AutoUninstrCallSiteTest {
    @Test public void uniqueMissingCallSite() {
        final Fiber f1 = new Fiber(new F()).start();
        try {
            f1.join();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber f2 = new Fiber(new F()).start();
        try {
            f2.join();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
