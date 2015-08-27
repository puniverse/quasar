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
package co.paralleluniverse.galaxy.example.issues;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestMessage;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Grid;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main111 {
    private static final Logger log = LoggerFactory.getLogger(Main111.class);
    public static final String ACTOR_NAME = "my-actor";
    private static final int nodeId = 1;

    public static void main(String[] args) throws Exception {
        final Properties props = new Properties();
        props.setProperty("galaxy.nodeId", Integer.toString(nodeId));
        props.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
        props.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));

        final Grid grid = Grid.getInstance(null, props);
        grid.goOnline();

        final MyActor actor = new MyActor();
        actor.spawnThread();
        actor.register(ACTOR_NAME);

        final ActorRef<HelloMessage> ref = ActorRegistry.getActor(ACTOR_NAME);

        try {
            RequestReplyHelper.call(ref, new HelloMessage());
        } catch (Throwable e) {
            log.error("====>", e);
        }

        grid.cluster().goOffline();
    }

    public static class MyActor extends BasicActor<HelloMessage, Void> {
        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            final HelloMessage message = receive();
            System.out.println("Message received '" + message + '\'');
            RequestReplyHelper.reply(message, 1);
            return null;
        }
    }

    public static class HelloMessage extends RequestMessage<Integer> {

        @Override
        public String toString() {
            return "Hello!";
        }
    }
}
