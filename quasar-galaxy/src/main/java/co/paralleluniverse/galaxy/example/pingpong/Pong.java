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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eitan
 */
public class Pong {
    private static final int nodeId = 2;

    public static void main(String[] args) {
        int pings = runPong();
        System.out.println(pings + " pings received");
    }

    public static int runPong()  {
        try {
            System.setProperty("galaxy.nodeId", Integer.toString(nodeId));
            System.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
            System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));
            
            ActorRegistry.hasGlobalRegistry();
            ActorRef<Message> pong = new BasicActor<Message, Integer>() {
                @Override
                protected Integer doRun() throws InterruptedException, SuspendExecution {
                    register("pong");
                    int pings = 0;
                    
                    System.out.println("Pong started");
                    loop:
                    while (true) {
                        Message msg = receive();
                        System.out.println("pong received " + msg.type);
                        switch (msg.type) {
                            case PING:
                                pings++;
                                msg.from.send(new Message(self(), PONG));
                                break;
                            case FINISHED:
                                break loop;
                        }
                    }
                    return pings;
                }
            }.spawn();
            int pings = LocalActor.get(pong);
            System.out.println("finished pong");
            Thread.sleep(100);
            ActorRegistry.shutdown();
            return pings;
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
