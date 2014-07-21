/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.common.io.Persistable;
import co.paralleluniverse.common.io.Streamable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import co.paralleluniverse.galaxy.CacheListener;
import co.paralleluniverse.galaxy.ItemState;
import co.paralleluniverse.galaxy.LineFunction;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.strands.Strand;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author pron
 */
public class StoreImpl implements Store {
    private final co.paralleluniverse.galaxy.Store store;

    public StoreImpl(co.paralleluniverse.galaxy.Store store) {
        this.store = store;
    }

    @Override
    public long alloc(int count, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.allocAsync(count, txn));
    }

    @Override
    public long put(byte[] data, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.putAsync(data, txn));
    }

    @Override
    public long put(ByteBuffer data, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.putAsync(data, txn));
    }

    @Override
    public long put(Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.putAsync(object, txn));
    }

    @Override
    public byte[] get(long id) throws TimeoutException, SuspendExecution {
        return result(store.getAsync(id));
    }

    @Override
    public void get(long id, Persistable object) throws TimeoutException, SuspendExecution {
        result(store.getAsync(id, object));
    }

    @Override
    public byte[] get(long id, short nodeHint) throws TimeoutException, SuspendExecution {
        return result(store.getAsync(id, nodeHint));
    }

    @Override
    public void get(long id, short nodeHint, Persistable object) throws TimeoutException, SuspendExecution {
        result(store.getAsync(id, nodeHint, object));
    }

    @Override
    public byte[] getFromOwner(long id, long ownerOf) throws TimeoutException, SuspendExecution {
        return result(store.getFromOwnerAsync(id, ownerOf));
    }

    @Override
    public void getFromOwner(long id, long ownerOf, Persistable object) throws TimeoutException, SuspendExecution {
        result(store.getFromOwnerAsync(id, ownerOf, object));
    }

    @Override
    public byte[] gets(long id, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.getsAsync(id, txn));
    }

    @Override
    public void gets(long id, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.getsAsync(id, object, txn));
    }

    @Override
    public byte[] gets(long id, short nodeHint, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.getsAsync(id, nodeHint, txn));
    }

    @Override
    public void gets(long id, short nodeHint, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.getsAsync(id, nodeHint, object, txn));
    }

    @Override
    public byte[] getsFromOwner(long id, long ownerOf, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.getsFromOwnerAsync(id, ownerOf, txn));
    }

    @Override
    public void getsFromOwner(long id, long ownerOf, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.getsFromOwnerAsync(id, ownerOf, object, txn));
    }

    @Override
    public byte[] getx(long id, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.getxAsync(id, txn));
    }

    @Override
    public void getx(long id, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.getxAsync(id, object, txn));
    }

    @Override
    public byte[] getx(long id, short nodeHint, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.getxAsync(id, nodeHint, txn));
    }

    @Override
    public void getx(long id, short nodeHint, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.getxAsync(id, nodeHint, object, txn));
    }

    @Override
    public byte[] getxFromOwner(long id, long ownerOf, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        return result(store.getxFromOwnerAsync(id, ownerOf, txn));
    }

    @Override
    public void getxFromOwner(long id, long ownerOf, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.getxFromOwnerAsync(id, ownerOf, object, txn));
    }

    @Override
    public void set(long id, byte[] data, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.setAsync(id, data, txn));
    }

    @Override
    public void set(long id, ByteBuffer data, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.setAsync(id, data, txn));
    }

    @Override
    public void set(long id, Persistable object, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.setAsync(id, object, txn));
    }

    @Override
    public <T> T invoke(long id, LineFunction<T> function) throws TimeoutException, SuspendExecution {
        return result(store.invokeAsync(id, function));
    }

    @Override
    public void del(long id, StoreTransaction txn) throws TimeoutException, SuspendExecution {
        result(store.delAsync(id, txn));
    }

    @Override
    public int getMaxItemSize() {
        return store.getMaxItemSize();
    }

    @Override
    public StoreTransaction beginTransaction() {
        return store.beginTransaction();
    }

    @Override
    public void commit(StoreTransaction txn) throws InterruptedException {
        store.commit(txn);
    }

    @Override
    public void abort(StoreTransaction txn) throws InterruptedException {
        store.abort(txn);
    }

    @Override
    public void rollback(StoreTransaction txn) {
        store.rollback(txn);
    }

    @Override
    public void release(long id) {
        store.release(id);
    }

    @Override
    public long getRoot(String rootName, StoreTransaction txn) throws TimeoutException {
        return store.getRoot(rootName, txn);
    }

    @Override
    public long getRoot(String rootName, long id, StoreTransaction txn) throws TimeoutException {
        return store.getRoot(rootName, id, txn);
    }

    @Override
    public boolean isRootCreated(long rootId, StoreTransaction txn) {
        return store.isRootCreated(rootId, txn);
    }

    @Override
    public void setListener(long id, CacheListener listener) {
        store.setListener(id, listener);
    }

    @Override
    public CacheListener setListenerIfAbsent(long id, CacheListener listener) {
        return store.setListenerIfAbsent(id, listener);
    }
    
    @Override
    public void push(long id, short... toNodes) {
        store.push(id, toNodes);
    }

    @Override
    public void pushx(long id, short toNode) {
        store.pushx(id, toNode);
    }

    @Override
    public boolean isPinned(long id) {
        return store.isPinned(id);
    }

    @Override
    public ItemState getState(long id) {
        return store.getState(id);
    }

    @Override
    public void send(long id, Streamable msg) throws TimeoutException, SuspendExecution {
        result(store.sendAsync(id, msg));
    }

    @Override
    public void send(long id, byte[] msg) throws TimeoutException, SuspendExecution {
        result(store.sendAsync(id, msg));
    }

    private <V> V result(ListenableFuture<V> future) throws TimeoutException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(future);
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();
            if (ex instanceof TimeoutException)
                throw (TimeoutException) ex;
            Throwables.propagateIfPossible(ex);
            throw Throwables.propagate(ex);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
            return null;
        }
    }
}
