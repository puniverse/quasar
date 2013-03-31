/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.io;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author pron
 */
public class LightweightThreadFileChannel implements LightweightThreadByteChannel {
    private final AsynchronousFileChannel ac;
    private long position;

    private LightweightThreadFileChannel(AsynchronousFileChannel afc) {
        ac = afc;
    }

    public static LightweightThreadFileChannel open(ExecutorService ioExecutor, Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return new LightweightThreadFileChannel(AsynchronousFileChannel.open(path, options, ioExecutor, attrs));
    }

    public static LightweightThreadFileChannel open(Path path, OpenOption... options) throws IOException {
        return new LightweightThreadFileChannel(AsynchronousFileChannel.open(path, options));
    }

    public long position() throws IOException {
        return position;
    }

    public LightweightThreadFileChannel position(long newPosition) throws IOException {
        this.position = newPosition;
        return this;
    }

    public int read(final ByteBuffer dst, final long position) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Integer>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Integer, LightweightThread> completionHandler) {
                ac.read(dst, position, current, completionHandler);
            }
        }.run();
    }

    public int write(final ByteBuffer src, final long position) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Integer>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Integer, LightweightThread> completionHandler) {
                ac.write(src, position, current, completionHandler);
            }
        }.run();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException, SuspendExecution {
        final int bytes = read(dst, position);
        position(position + bytes);
        return bytes;
    }

    @Override
    public int write(ByteBuffer src) throws IOException, SuspendExecution {
        final int bytes = write(src, position);
        position(position + bytes);
        return bytes;
    }

    @Override
    public boolean isOpen() {
        return ac.isOpen();
    }

    @Override
    public void close() throws IOException {
        ac.close();
    }
    
    public long size() throws IOException {
        return ac.size();
    }

    public void force(boolean metaData) throws IOException {
        ac.force(metaData);
    }

    public LightweightThreadFileChannel truncate(long size) throws IOException {
        ac.truncate(size);
        return this;
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return ac.tryLock(position, size, shared);
    }

    public final FileLock tryLock() throws IOException {
        return ac.tryLock();
    }

    public final FileLock lock() throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<FileLock>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<FileLock, LightweightThread> completionHandler) {
                ac.lock(current, completionHandler);
            }
        }.run();
    }

    public FileLock lock(final long position, final long size, final boolean shared) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<FileLock>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<FileLock, LightweightThread> completionHandler) {
                ac.lock(position, size, shared, current, completionHandler);
            }
        }.run();
    }
}
