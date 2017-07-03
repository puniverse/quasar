package co.paralleluniverse.kotlin.fibers.lang;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.junit.Ignore;
import org.junit.Test;

import static co.paralleluniverse.kotlin.fibers.lang.SAMSupportKt.accept;

public class SAMTest {
    @Test
    @Ignore
    // TODO https://github.com/puniverse/quasar/issues/275
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
