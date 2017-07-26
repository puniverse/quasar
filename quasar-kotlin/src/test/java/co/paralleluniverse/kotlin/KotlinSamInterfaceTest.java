package co.paralleluniverse.kotlin;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.junit.Test;

import static co.paralleluniverse.kotlin.KotlinSamInterfaceKt.accept;

public class KotlinSamInterfaceTest {

    @Test
    public void testSamConvertedLambda() throws Exception {
        new Fiber() {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                accept(() -> {
                    try {
                        Strand.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                return null;
            }
        }.start().join();

    }
}
