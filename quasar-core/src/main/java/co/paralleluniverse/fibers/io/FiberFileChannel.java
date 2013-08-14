/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author pron
 */
public class FiberFileChannel extends FileChannel {
    private final AsynchronousFileChannel ac;
    private long position;

    private FiberFileChannel(AsynchronousFileChannel afc) {
        ac = afc;
    }

    public static FiberFileChannel open(ExecutorService ioExecutor, Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return new FiberFileChannel(AsynchronousFileChannel.open(path, options, ioExecutor, attrs));
    }

    public static FiberFileChannel open(Path path, OpenOption... options) throws IOException {
        return new FiberFileChannel(AsynchronousFileChannel.open(path, options));
    }

    @Override
    protected void implCloseChannel() throws IOException {
        ac.close();
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public FiberFileChannel position(long newPosition) throws IOException {
        this.position = newPosition;
        return this;
    }

    @Override
    @Suspendable
    public int read(final ByteBuffer dst, final long position) throws IOException {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected Void requestAsync(Fiber current, CompletionHandler<Integer, Fiber> completionHandler) {
                ac.read(dst, position, current, completionHandler);
                return null;
            }
        }.runSneaky();
    }

    @Override
    @Suspendable
    public int write(final ByteBuffer src, final long position) throws IOException {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected Void requestAsync(Fiber current, CompletionHandler<Integer, Fiber> completionHandler) {
                ac.write(src, position, current, completionHandler);
                return null;
            }
        }.runSneaky();
    }

    @Override
    @Suspendable
    public FileLock lock(final long position, final long size, final boolean shared) throws IOException {
        return new FiberAsyncIO<FileLock>() {
            @Override
            protected Void requestAsync(Fiber current, CompletionHandler<FileLock, Fiber> completionHandler) {
                ac.lock(position, size, shared, current, completionHandler);
                return null;
            }
        }.runSneaky();
    }

    @Override
    @Suspendable
    public int read(ByteBuffer dst) throws IOException {
        final int bytes = read(dst, position);
        position(position + bytes);
        return bytes;
    }

    @Override
    @Suspendable
    public int write(ByteBuffer src) throws IOException {
        final int bytes = write(src, position);
        position(position + bytes);
        return bytes;
    }

    @Override
    public long size() throws IOException {
        return ac.size();
    }

    @Override
    public void force(boolean metaData) throws IOException {
        ac.force(metaData);
    }

    @Override
    public FiberFileChannel truncate(long size) throws IOException {
        ac.truncate(size);
        return this;
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return ac.tryLock(position, size, shared);
    }

    @Override
    @Suspendable
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        long r = 0;
        for (int i = 0; i < length; i++)
            r += read(dsts[offset + i]);
        return r;
    }

    @Override
    @Suspendable
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        long r = 0;
        for (int i = 0; i < length; i++)
            r += write(srcs[offset + i]);
        return r;
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new UnsupportedOperationException();
    }
}
