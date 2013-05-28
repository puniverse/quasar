/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.actors.SelectiveReceiveHelper;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class RequestReplyHelper {
    public static GenResponseMessage call(Actor actor, GenRequestMessage m, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        final SelectiveReceiveHelper<Object> helper = new SelectiveReceiveHelper<>(LocalActor.currentActor());
        final Object id = m.getId();

        actor.sendSync(m);
        final GenResponseMessage response = (GenResponseMessage) helper.receive(timeout, unit, new MessageProcessor<Object>() {
            @Override
            public boolean process(Object m) throws SuspendExecution, InterruptedException {
                return (m instanceof GenResponseMessage && id.equals(((GenResponseMessage) m).getId()));
            }
        });

        if (response instanceof GenErrorResponseMessage)
            throw Exceptions.rethrow(((GenErrorResponseMessage) response).getError());
        return response;
    }

    public static GenResponseMessage call(Actor actor, GenRequestMessage m) throws InterruptedException, SuspendExecution {
        return call(actor, m, 0, null);
    }
}
