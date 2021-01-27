/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

/**
 *
 * @author Elias Naur
 */
public class InterfaceTest {
    interface SomeInterface {
        void doStuff() throws SuspendExecution;
    }

    public class C2 implements SomeInterface {
        @Override
        public void doStuff() throws SuspendExecution {
        }
    }

    public class C implements SomeInterface {
        @Override
        public void doStuff() throws SuspendExecution {
            /*			float time = 0f;
             float seconds = .8f;
             do {
             float t = .06667f;
             System.out.println("time = " + time + " " + (time + t));
             //          time = StrictMath.min(time + t, seconds);
             time = time + t;
             System.out.println("seconds = " + seconds + " | time = " + time + " | t = " + t);
             System.out.println("this = " + this);
 
             System.out.println("time just not after = " + time);
             Coroutine.yield();
             System.out.println("time after = " + time);
             } while (time < seconds);
             System.out.println("1 = " + 1);*/
        }
    }

    @Test
    public void testSuspend() {
//		final I i = new C();
        Fiber co = new Fiber((String)null, null, new SuspendableRunnable() {
            @Override
            public final void run() throws SuspendExecution {
                // next line causes an error because of incomplete merge in TypeInterpreter
                //SomeInterface i = System.currentTimeMillis() > 0 ? new C() : new C2();
                SomeInterface i = new C();
                System.out.println("i = " + i);
                i.doStuff();
            }
        });
        while (!TestsHelper.exec(co))
            System.out.println("State=" + co.getState());
        System.out.println("State=" + co.getState());
    }
}