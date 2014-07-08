/*
 * Copyright (c) 2013-2014 Parallel Universe Software Co.
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package co.paralleluniverse.galaxy.example.migration;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.MigratingActor;
import co.paralleluniverse.fibers.SuspendExecution;
import static co.paralleluniverse.galaxy.example.migration.Message.Type.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        final int nodeId = 1; // Integer.parseInt(args[0]);
        System.setProperty("galaxy.nodeId", Integer.toString(nodeId));
        System.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));

        // com.esotericsoftware.minlog.Log.set(1);
        
        ActorRegistry.hasGlobalRegistry();
        ActorRef<Message> actor = ActorRegistry.getActor("migrant");
        if (actor == null) {
            System.out.println("Creating actor");
            actor = new Migrant("migrant").spawn();
        } else
            System.out.println("Found registered actor");

        for (int i = 0; i < 100; i++) {
            final double r = ThreadLocalRandom.current().nextDouble();
            if (r < 0.2) {
                actor.send(new Message(null, MIGRATE));
                System.out.println("Hiring actor...");
                Thread.sleep(500);
                Actor.hire(actor).spawn();
                System.out.println("Hired!");
            } else
                actor.send(new Message(null, PRINT));
            Thread.sleep(1000);
        }
        actor.send(new Message(null, FINISHED));

        System.out.println("Done");
        ActorRegistry.shutdown();
    }

    static class Migrant extends BasicActor<Message, Void> implements MigratingActor {
        private int loopCount;
        private int messageCount;

        public Migrant(String name) {
            super(name, null);
        }

        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            register();
            loop:
            for (;;) {
                Message m = receive(2, TimeUnit.SECONDS);
                if (m != null) {
                    messageCount++;
                    switch (m.type) {
                        case PRINT:
                            System.out.println("iter: " + loopCount + " messages: " + messageCount);
                            break;
                        case MIGRATE:
                            migrateAndRestart();
                            return null;
                        case FINISHED:
                            System.out.println("done");
                            break loop;
                    }
                }
                loopCount++;
            }
            return null;
        }
    }
}
