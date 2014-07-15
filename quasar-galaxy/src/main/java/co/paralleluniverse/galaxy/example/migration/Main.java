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
import co.paralleluniverse.actors.behaviors.Server;
import co.paralleluniverse.actors.behaviors.ServerActor;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import static co.paralleluniverse.galaxy.example.migration.Message.Type.*;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        final int nodeId = Integer.parseInt(args[0]);
        System.setProperty("galaxy.nodeId", Integer.toString(nodeId));
        System.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));

        // com.esotericsoftware.minlog.Log.set(1);
        ActorRegistry.hasGlobalRegistry();
//        final ActorRef<Message> actor = ActorRegistry.getOrRegisterActor("migrant", new Callable<Actor<Message, ?>>() {
//
//            @Override
//            public Actor<Message, Void> call() throws Exception {
//                return new Migrant();
//            }
//        });
        final Server<Message, Integer, Message> actor = (Server<Message, Integer, Message>) ActorRegistry.getOrRegisterActor("migrant", new Callable() {

            @Override
            public ServerActor call() throws Exception {
                return new Migrant();
            }
        });

        int i;
        for (i = 0; i < 500; i++) {
            final double r = ThreadLocalRandom.current().nextDouble();
            if (r < 0.1) {
                System.out.println("Hiring actor...");
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Strand.sleep(1000);
                            System.out.println("22222");
                            Actor.hire(actor);
                            System.out.println("Hired!");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.start();

                actor.call(new Message(nodeId, i, MIGRATE));
                // actor.send(new Message(nodeId, i, MIGRATE));
            } else {
                // actor.send(new Message(nodeId, i, PRINT));
                actor.cast(new Message(nodeId, i, PRINT));
            }
            Thread.sleep(500);
        }
        // actor.send(new Message(nodeId, i, FINISHED));
        actor.cast(new Message(nodeId, i, FINISHED));

        System.out.println("Done");
        ActorRegistry.shutdown();
    }

//    static class Migrant extends BasicActor<Message, Void> implements MigratingActor {
//        private int loopCount;
//        private int messageCount;
//
//        public Migrant() {
//            super();
//        }
//
//        @Override
//        protected Void doRun() throws InterruptedException, SuspendExecution {
//            loop:
//            for (;;) {
//                Message m = receive(2, TimeUnit.SECONDS);
//                if (m != null) {
//                    System.out.println("received: " + m);
//                    messageCount++;
//                    switch (m.type) {
//                        case PRINT:
//                            System.out.println("iter: " + loopCount + " messages: " + messageCount);
//                            break;
//                        case MIGRATE:
//                            migrateAndRestart();
//                            return null;
//                        case FINISHED:
//                            System.out.println("done");
//                            break loop;
//                    }
//                }
//                loopCount++;
//            }
//            return null;
//        }
//    }
    static class Migrant extends ServerActor<Message, Integer, Message> implements MigratingActor {
        private int loopCount;
        private int messageCount;

        public Migrant() {
            super();
        }

        @Override
        protected void init() throws InterruptedException, SuspendExecution {
            setTimeout(2, TimeUnit.SECONDS);
        }

        @Override
        protected void handleMessage(Object m1) throws InterruptedException, SuspendExecution {
            messageCount++;
            loopCount++;
            super.handleMessage(m1);
        }

        @Override
        protected void handleCast(ActorRef<?> from, Object id, Message m) throws SuspendExecution {
            switch (m.type) {
                case PRINT:
                    System.out.println("iter: " + loopCount + " messages: " + messageCount);
                    break;
                case FINISHED:
                    shutdown();
            }
        }

        @Override
        protected Integer handleCall(ActorRef<?> from, Object id, Message m) throws Exception, SuspendExecution {
            switch (m.type) {
                case MIGRATE:
                    System.out.println("111111");
                    migrate();
                    return messageCount;
                default:
                    throw new UnsupportedOperationException(m.toString());
            }
        }

        @Override
        protected void handleTimeout() throws SuspendExecution {
            loopCount++;
        }
    }
}
