/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
public abstract class Channel<Message> implements SendChannel<Message> {
    private final Object owner;
    private final boolean lwt;
    private final Lock lock = new ReentrantLock();
    private final Condition queueNotEmpty = lock.newCondition();
    final SingleConsumerQueue<Message, Object> queue;

    Channel(Thread owner, SingleConsumerQueue<Message, ?> queue) {
        this((Object) owner, queue);
    }

    Channel(LightweightThread owner, SingleConsumerQueue<Message, ?> queue) {
        this((Object) owner, queue);
    }

    private Channel(Object owner, SingleConsumerQueue<Message, ?> queue) {
        this.queue = (SingleConsumerQueue<Message, Object>) queue;
        this.owner = owner;
        this.lwt = owner instanceof LightweightThread;
    }

    public Object getOwner() {
        return owner;
    }

    void verifyOwner() {
        assert owner == (lwt ? LightweightThread.currentLightweightThread() : Thread.currentThread());
    }

    boolean isOwnerAlive() {
        return lwt ? lwtOwner().isAlive() : threadOwner().isAlive();
    }

    private LightweightThread lwtOwner() {
        return (LightweightThread) owner;
    }

    private Thread threadOwner() {
        return (Thread) owner;
    }

    void lock() {
        if (!lwt)
            lock.lock();
    }

    void unlock() {
        if (!lwt)
            lock.unlock();
    }

    void notifyOwner() {
        if (lwt)
            lwtOwner().unpark();
        else
            queueNotEmpty.signal();
    }

    void notifyOwnerAndTryToExecNow() {
        if (lwt) {
            if (!lwtOwner().exec(this))
                lwtOwner().unpark();
        } else
            notifyOwner();
    }

    void await() throws InterruptedException, SuspendExecution {
        if (lwt)
            LightweightThread.park(this);
        else
            queueNotEmpty.await();
    }

    void await(Object blocker, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        if (lwt)
            LightweightThread.park(this, timeout, unit);
        else
            queueNotEmpty.await(timeout, unit);
    }

    public void send(Message message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(Message message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }

    Object receiveNode() throws SuspendExecution, InterruptedException {
        verifyOwner();
        Object n;
        lock();
        try {
            while ((n = queue.pk()) == null)
                await();
        } finally {
            unlock();
        }
        return n;
    }

    public Message receive() throws SuspendExecution, InterruptedException {
        verifyOwner();
        return queue.value(receiveNode());
    }
}
