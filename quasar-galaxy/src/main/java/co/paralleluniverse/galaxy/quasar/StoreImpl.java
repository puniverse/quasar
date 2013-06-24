/*
 * Galaxy
 * Copyright (C) 2012 Parallel Universe Software Co.
 * 
 * This file is part of Galaxy.
 *
 * Galaxy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * Galaxy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Galaxy. If not, see <http://www.gnu.org/licenses/>.
 */
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.common.io.Persistable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.FiberAsyncListenableFuture;
import co.paralleluniverse.galaxy.CacheListener;
import co.paralleluniverse.galaxy.InvokeOnLine;
import co.paralleluniverse.galaxy.ItemState;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.strands.Strand;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public long put(byte[] data, StoreTransaction txn) throws TimeoutException {
        return store.put(data, txn);
    }

    @Override
    public long put(ByteBuffer data, StoreTransaction txn) throws TimeoutException {
        return store.put(data, txn);
    }

    @Override
    public long put(Persistable object, StoreTransaction txn) throws TimeoutException {
        return store.put(object, txn);
    }

    public <T> T invoke(long id, InvokeOnLine<T> function) throws TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] get(long id) throws TimeoutException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(store.getAsync(id));
        } catch (ExecutionException e) {
            throw propagateException(e);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
            return null;
        }
    }

    @Override
    public void get(long id, Persistable object) throws TimeoutException, SuspendExecution {
        try {
            FiberAsyncListenableFuture.get(store.getAsync(id, object));
        } catch (ExecutionException e) {
            throw propagateException(e);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
        }
    }

    @Override
    public byte[] get(long id, short nodeHint) throws TimeoutException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(store.getAsync(id, nodeHint));
        } catch (ExecutionException e) {
            throw propagateException(e);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
            return null;
        }
    }

    @Override
    public void get(long id, short nodeHint, Persistable object) throws TimeoutException, SuspendExecution {
        try {
            FiberAsyncListenableFuture.get(store.getAsync(id, nodeHint, object));
        } catch (ExecutionException e) {
            throw propagateException(e);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
        }
    }

    @Override
    public byte[] getFromOwner(long id, long ownerOf) throws TimeoutException, SuspendExecution {
        try {
            return FiberAsyncListenableFuture.get(store.getFromOwnerAsync(id, ownerOf));
        } catch (ExecutionException e) {
            throw propagateException(e);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
            return null;
        }
    }

    @Override
    public void getFromOwner(long id, long ownerOf, Persistable object) throws TimeoutException, SuspendExecution {
        try {
            FiberAsyncListenableFuture.get(store.getFromOwnerAsync(id, ownerOf, object));
        } catch (ExecutionException e) {
            throw propagateException(e);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
        }
    }

    public byte[] gets(long id, StoreTransaction txn) throws TimeoutException {
        return store.gets(id, txn);
    }

    public void gets(long id, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.gets(id, object, txn);
    }

    public byte[] gets(long id, short nodeHint, StoreTransaction txn) throws TimeoutException {
        return store.gets(id, nodeHint, txn);
    }

    public void gets(long id, short nodeHint, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.gets(id, nodeHint, object, txn);
    }

    public byte[] getsFromOwner(long id, long ownerOf, StoreTransaction txn) throws TimeoutException {
        return store.getsFromOwner(id, ownerOf, txn);
    }

    public void getsFromOwner(long id, long ownerOf, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.getsFromOwner(id, ownerOf, object, txn);
    }

    public byte[] getx(long id, StoreTransaction txn) throws TimeoutException {
        return store.getx(id, txn);
    }

    public void getx(long id, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.getx(id, object, txn);
    }

    public byte[] getx(long id, short nodeHint, StoreTransaction txn) throws TimeoutException {
        return store.getx(id, nodeHint, txn);
    }

    public void getx(long id, short nodeHint, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.getx(id, nodeHint, object, txn);
    }

    public byte[] getxFromOwner(long id, long ownerOf, StoreTransaction txn) throws TimeoutException {
        return store.getxFromOwner(id, ownerOf, txn);
    }

    public void getxFromOwner(long id, long ownerOf, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.getxFromOwner(id, ownerOf, object, txn);
    }

    public void set(long id, byte[] data, StoreTransaction txn) throws TimeoutException {
        store.set(id, data, txn);
    }

    public void set(long id, ByteBuffer data, StoreTransaction txn) throws TimeoutException {
        store.set(id, data, txn);
    }

    public void set(long id, Persistable object, StoreTransaction txn) throws TimeoutException {
        store.set(id, object, txn);
    }

    public void del(long id, StoreTransaction txn) throws TimeoutException {
        store.del(id, txn);
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
    public boolean isRootCreated(long rootId, StoreTransaction txn) {
        return store.isRootCreated(rootId, txn);
    }

    @Override
    public void setListener(long id, CacheListener listener) {
        store.setListener(id, listener);
    }

    @Override
    public long alloc(int count, StoreTransaction txn) throws TimeoutException {
        return store.alloc(count, txn);
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

    private RuntimeException propagateException(ExecutionException e) throws TimeoutException {
        Throwable ex = e.getCause();
        if (ex instanceof TimeoutException)
            throw (TimeoutException) ex;
        Throwables.propagateIfPossible(ex);
        throw Throwables.propagate(ex);
    }
}
