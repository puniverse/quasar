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
package co.paralleluniverse.galaxy.example.pingpong;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.SuspendExecution;
import static co.paralleluniverse.galaxy.example.pingpong.Message.Type.*;
import co.paralleluniverse.strands.Strand;

/**
 *
 * @author eitan
 */
public class Ping {
    private static final int nodeId = 1;

    public static void main(String[] args) throws Exception {
        System.setProperty("galaxy.nodeId", Integer.toString(nodeId));
        System.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));

        ActorRegistry.hasGlobalRegistry();
        ActorRef<Message> ping = new BasicActor<Message, Void>() {
            @Override
            protected Void doRun() throws InterruptedException, SuspendExecution {
                ActorRef pong;
                while ((pong = ActorRegistry.getActor("pong")) == null) {
                    System.out.println("waiting for pong");
                    Strand.sleep(3000);
                }
                System.out.println("pong is " + pong);

                for (int i = 0; i < 3; i++) {
                    pong.send(new Message(self(), PING));
                    Message msg = receive();
                    System.out.println("ping received " + msg.type);
                }

                pong.send(new Message(null, FINISHED));
                return null;
            }
        }.spawn();
        LocalActor.join(ping);
        System.out.println("finished ping");
        System.exit(0);
    }
}
