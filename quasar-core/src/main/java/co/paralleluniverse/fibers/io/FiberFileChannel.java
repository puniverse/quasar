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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Uses an {@link AsynchronousFileChannel} to implement a fiber-blocking version of {@link FileChannel}.
 *
 * @author pron
 */
public class FiberFileChannel extends FileChannel {
    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute[0];
    
    private final AsynchronousFileChannel ac;
    private long position;

    public FiberFileChannel(AsynchronousFileChannel afc) {
        ac = afc;
    }

    /**
     * Opens or creates a file for reading and/or writing, returning a file channel to access the file.
     *
     * <p> The {@code options} parameter determines how the file is opened.
     * The {@link StandardOpenOption#READ READ} and {@link StandardOpenOption#WRITE
     * WRITE} options determines if the file should be opened for reading and/or
     * writing. If neither option is contained in the array then an existing file
     * is opened for reading.
     *
     * <p> In addition to {@code READ} and {@code WRITE}, the following options
     * may be present:
     *
     * <table border=1 cellpadding=5 summary="">
     * <tr> <th>Option</th> <th>Description</th> </tr>
     * <tr>
     * <td> {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} </td>
     * <td> When opening an existing file, the file is first truncated to a
     * size of 0 bytes. This option is ignored when the file is opened only
     * for reading.</td>
     * </tr>
     * <tr>
     * <td> {@link StandardOpenOption#CREATE_NEW CREATE_NEW} </td>
     * <td> If this option is present then a new file is created, failing if
     * the file already exists. When creating a file the check for the
     * existence of the file and the creation of the file if it does not exist
     * is atomic with respect to other file system operations. This option is
     * ignored when the file is opened only for reading. </td>
     * </tr>
     * <tr>
     * <td > {@link StandardOpenOption#CREATE CREATE} </td>
     * <td> If this option is present then an existing file is opened if it
     * exists, otherwise a new file is created. When creating a file the check
     * for the existence of the file and the creation of the file if it does
     * not exist is atomic with respect to other file system operations. This
     * option is ignored if the {@code CREATE_NEW} option is also present or
     * the file is opened only for reading. </td>
     * </tr>
     * <tr>
     * <td > {@link StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} </td>
     * <td> When this option is present then the implementation makes a
     * <em>best effort</em> attempt to delete the file when closed by the
     * the {@link #close close} method. If the {@code close} method is not
     * invoked then a <em>best effort</em> attempt is made to delete the file
     * when the Java virtual machine terminates. </td>
     * </tr>
     * <tr>
     * <td>{@link StandardOpenOption#SPARSE SPARSE} </td>
     * <td> When creating a new file this option is a <em>hint</em> that the
     * new file will be sparse. This option is ignored when not creating
     * a new file. </td>
     * </tr>
     * <tr>
     * <td> {@link StandardOpenOption#SYNC SYNC} </td>
     * <td> Requires that every update to the file's content or metadata be
     * written synchronously to the underlying storage device. (see <a
     * href="../file/package-summary.html#integrity"> Synchronized I/O file
     * integrity</a>). </td>
     * <tr>
     * <tr>
     * <td> {@link StandardOpenOption#DSYNC DSYNC} </td>
     * <td> Requires that every update to the file's content be written
     * synchronously to the underlying storage device. (see <a
     * href="../file/package-summary.html#integrity"> Synchronized I/O file
     * integrity</a>). </td>
     * </tr>
     * </table>
     *
     * <p> An implementation may also support additional options.
     *
     * <p> The {@code executor} parameter is the {@link ExecutorService} to
     * which tasks are submitted to handle I/O events and dispatch completion
     * results for operations initiated on resulting channel.
     * The nature of these tasks is highly implementation specific and so care
     * should be taken when configuring the {@code Executor}. Minimally it
     * should support an unbounded work queue and should not run tasks on the
     * caller thread of the {@link ExecutorService#execute execute} method.
     * Shutting down the executor service while the channel is open results in
     * unspecified behavior.
     *
     * <p> The {@code attrs} parameter is an optional array of file {@link
     * FileAttribute file-attributes} to set atomically when creating the file.
     *
     * <p> The new channel is created by invoking the {@link
     * FileSystemProvider#newFileChannel newFileChannel} method on the
     * provider that created the {@code Path}.
     *
     * @param path       The path of the file to open or create
     * @param options    Options specifying how the file is opened
     * @param ioExecutor The thread pool or {@code null} to associate the channel with the default thread pool
     * @param attrs      An optional list of file attributes to set atomically when creating the file
     *
     * @return A new file channel
     *
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If the {@code file} is associated with a provider that does not
     *                                       support creating asynchronous file channels, or an unsupported
     *                                       open option is specified, or the array contains an attribute that
     *                                       cannot be set atomically when creating the file
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             If a security manager is installed and it denies an
     *                                       unspecified permission required by the implementation.
     *                                       In the case of the default provider, the {@link SecurityManager#checkRead(String)}
     *                                       method is invoked to check read access if the file is opened for reading.
     *                                       The {@link SecurityManager#checkWrite(String)} method is invoked to check
     *                                       write access if the file is opened for writing
     */
    public static FiberFileChannel open(ExecutorService ioExecutor, Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return new FiberFileChannel(AsynchronousFileChannel.open(path, options, ioExecutor != null ? ioExecutor : FiberAsyncIO.ioExecutor(), attrs));
    }

    /**
     * Opens or creates a file for reading and/or writing, returning a file channel to access the file.
     *
     * <p> An invocation of this method behaves in exactly the same way as the
     * invocation
     * <pre>
     *     ch.{@link #open(Path,Set,ExecutorService,FileAttribute[])
     *       open}(file, opts, null, new FileAttribute&lt;?&gt;[0]);
     * </pre>
     * where {@code opts} is a {@code Set} containing the options specified to
     * this method.
     *
     * <p> The resulting channel is associated with default thread pool to which
     * tasks are submitted to handle I/O events and dispatch to completion
     * handlers that consume the result of asynchronous operations performed on
     * the resulting channel.
     *
     * @param path    The path of the file to open or create
     * @param options Options specifying how the file is opened
     *
     * @return A new file channel
     *
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If the {@code file} is associated with a provider that does not
     *                                       support creating file channels, or an unsupported open option is
     *                                       specified
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             If a security manager is installed and it denies an
     *                                       unspecified permission required by the implementation.
     *                                       In the case of the default provider, the {@link SecurityManager#checkRead(String)}
     *                                       method is invoked to check read access if the file is opened for reading.
     *                                       The {@link SecurityManager#checkWrite(String)} method is invoked to check
     *                                       write access if the file is opened for writing
     */
    public static FiberFileChannel open(Path path, OpenOption... options) throws IOException {
        Set<OpenOption> set = new HashSet<OpenOption>(options.length);
        Collections.addAll(set, options);

        return new FiberFileChannel(AsynchronousFileChannel.open(path, set, FiberAsyncIO.ioExecutor(), NO_ATTRIBUTES));
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
            protected void requestAsync() {
                ac.read(dst, position, null, makeCallback());
            }
        }.runSneaky();
    }

    @Override
    @Suspendable
    public int write(final ByteBuffer src, final long position) throws IOException {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected void requestAsync() {
                ac.write(src, position, null, makeCallback());
            }
        }.runSneaky();
    }

    @Override
    @Suspendable
    public FileLock lock(final long position, final long size, final boolean shared) throws IOException {
        return new FiberAsyncIO<FileLock>() {
            @Override
            protected void requestAsync() {
                ac.lock(position, size, shared, null, makeCallback());
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
