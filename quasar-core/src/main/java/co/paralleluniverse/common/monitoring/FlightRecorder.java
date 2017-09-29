/*
 * Copyright (c) 2011-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.common.monitoring;

import co.paralleluniverse.concurrent.util.MapUtil;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author pron
 */
public class FlightRecorder extends SimpleMBean implements FlightRecorderMXBean {
    private static final int DEFAULT_SIZE = Integer.getInteger("co.paralleluniverse.monitoring.flightRecorderSize", 20000);
    private static final int DEFAULT_LEVEL = Integer.getInteger("co.paralleluniverse.monitoring.flightRecorderLevel", 5);
    private final ConcurrentMap<Thread, ThreadRecorder> recorders = MapUtil.newConcurrentHashMap();
    //private final ThreadLocal<ThreadRecorder> threadRecorder = new ThreadLocal<ThreadRecorder>();
    private final long startWallTime;
    private final long startTimestamp;
    private boolean recording = true;
    private Object aux;

    public FlightRecorder(String name) {
        super(null, name, "FlightRecorder", null);
        startTimestamp = System.nanoTime();
        startWallTime = System.currentTimeMillis();
        registerMBean(true);
    }

    public void clear() {
        recorders.clear();
    }

    public void setAux(Object aux) {
        this.aux = aux;
    }

    public ThreadRecorder init(int size, int level) {
        if (!recording)
            return null;
        ThreadRecorder recorder = recorders.get(Thread.currentThread()); // threadRecorder.get();
        if (recorder != null) {
            if (recorder.timestamps.length != size)
                System.err.println("Flight recorder already initialized for thread " + Thread.currentThread() + " with size " + recorder.timestamps.length + ", which is different from the requested size of " + size);
        } else {
            recorder = new ThreadRecorder(size, level, aux);
            //threadRecorder.set(recorder);
            recorders.put(Thread.currentThread(), recorder);
        }
        System.err.println("STARTING FLIGHT RECORDER FOR THREAD " + Thread.currentThread() + " OF SIZE " + size + " AT LEVEL " + level);
        return recorder;
    }

    public ThreadRecorder get() {
        ThreadRecorder recorder = recorders.get(Thread.currentThread()); // threadRecorder.get();
        if (recorder == null)
            return init(DEFAULT_SIZE, DEFAULT_LEVEL);
        else
            return recorder;
    }

    public void record(int level, Object payload) {
        final ThreadRecorder recorder = get();
        if (recorder != null)
            recorder.record(level, payload);
    }

    public void record(int level, Object... payload) {
        final ThreadRecorder recorder = get();
        if (recorder != null)
            recorder.record(level, payload);
    }
    //////////////////////////////////////////////

    public class ThreadRecorder {
        private final Thread myThread;
        private Object aux;
        private final int level;
        //private final long startTime;
        //private final long startTimestamp;
        private final long[] timestamps;
        private final Object[] payloads;
        private long totalRecs;
        private int head; // points to earliest entry available for reading
        private int tail; // points to slot where next record will be written
//    private volatile boolean sync = true;
//    private boolean recording;

        private ThreadRecorder(int size, int level, Object aux) {
            this.myThread = Thread.currentThread();
            this.aux = aux;
            this.level = level;
            timestamps = new long[size];
            payloads = new Object[size];
            head = 0;
            tail = 0;
            totalRecs = 0;
            //startTimestamp = System.nanoTime();
            //startTime = System.currentTimeMillis();
//        recording = true;
        }

        public boolean recordsLevel(int level) {
            return level <= this.level;
        }

        public void setAux(Object aux) {
            this.aux = aux;
        }

        public Object getAux() {
            return aux;
        }

        public int numOfElements() {
            int n = tail - head;
            if (tail < head)
                n += timestamps.length;
            return n;
        }

        public long getTotalRecs() {
            return totalRecs;
        }

        private int next(int num) {
            num++;
            if (num == timestamps.length)
                num = 0;
            return num;
        }

        private boolean isLast(int i) {
            return next(i) == tail;
        }

        public Thread getThread() {
            return myThread;
        }
        
        public void record(int level, Object obj) {
            assert Thread.currentThread() == myThread : "my thread: " + myThread.getName() + " current thread: " + Thread.currentThread().getName();
            if (!recording)
                return;
            if (level > this.level)
                return;

//            if (obj instanceof FlightRecorderMessage) {
//                FlightRecorderMessage frm = (FlightRecorderMessage) obj;
//                if (!Objects.equals(frm.getClazz(), "ParkableForkJoinTask") && !Objects.equals(frm.getClazz(), "Fiber"))
//                    return;
//            }

            totalRecs++;
            timestamps[tail] = System.nanoTime();
            payloads[tail] = obj;
            tail = next(tail);
            if (tail == head)
                head = next(head);
//        sync = true;
        }

        public void record(int level, Object... objs) {
            record(level, (Object) objs);
        }
    }

    public void stop() {
        this.recording = false;
    }
    ////////////////////////////////////

    public Iterable<Record> getRecords() {
//        if (!sync)
//            throw new AssertionError();
        return new Iterable<Record>() {
            @Override
            public Iterator<Record> iterator() {
                return new Iterator<Record>() {
                    final Thread[] threads = recorders.keySet().toArray(new Thread[0]);
                    final int n = threads.length;
                    final ThreadRecorder[] trs = new ThreadRecorder[n];
                    final int[] is = new int[n];
                    final long[] ts = new long[n];
                    final Object[] ps = new Object[n];
                    int nextFr = -1;
                    long lastTimestamp;

                    {
                        for (int i = 0; i < n; i++) {
                            trs[i] = recorders.get(threads[i]);
                            is[i] = -1;
                            readNext(i);
                        }
                        nextFr = findMin();
                        lastTimestamp = -1;
                    }

                    private void readNext(int index) {
                        ThreadRecorder tr = trs[index];
                        int i = is[index];
                        if ((i < 0 && tr.numOfElements() == 0) || tr.isLast(i)) {
                            is[index] = -1;
                            ts[index] = Long.MAX_VALUE;
                            ps[index] = null;
                        } else {
                            i = (i < 0 ? tr.head : tr.next(i));
                            is[index] = i;
                            ts[index] = tr.timestamps[i];// - startTimestamp;
                            ps[index] = tr.payloads[i];
                        }
                    }

                    private int findMin() {
                        long min = Long.MAX_VALUE;
                        int minIndex = -1;
                        for (int i = 0; i < n; i++) {
                            if (ts[i] < min) {
                                min = ts[i];
                                minIndex = i;
                            }
                        }
                        return minIndex;
                    }

                    private Record createRecord(int index, long lastTimestamp) {
                        long time = startWallTime + (ts[index] - startTimestamp) / 1000000;
                        return new Record(threads[index], is[index], time, ps[index], time == lastTimestamp);
                    }

                    @Override
                    public boolean hasNext() {
                        return nextFr >= 0;
                    }

                    @Override
                    public Record next() {
                        if (!hasNext())
                            throw new NoSuchElementException();
                        Record r = createRecord(nextFr, lastTimestamp);
                        readNext(nextFr);
                        nextFr = findMin();
                        return r;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    private static final MessageFormat recordFormatter = new MessageFormat("[{0}{1} {2} ({3})]\t");

    public static class Record {
        public final Thread thread;
        public final int index;
        public final long timestamp;
        public final Object payload;
        public final boolean sameAsLast;

        private Record(Thread thread, int index, long timestamp, Object payload, boolean sameAsLast) {
            this.thread = thread;
            this.index = index;
            this.timestamp = timestamp;
            this.payload = payload;
            this.sameAsLast = sameAsLast;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(recordFormatter.format(new Object[]{
                        formatTimestamp(timestamp),
                        sameAsLast ? "*" : "",
                        thread.getName(),
                        index
                    }));
            if (payload == null)
                sb.append("NULL");
            else {
                try {
                    sb.append(payload instanceof Object[] ? Arrays.toString((Object[]) payload) : payload.toString());
                } catch (Exception e) {
                    sb.append("ERROR IN toString FOR THIS PAYLOAD");
                }
            }
            return sb.toString();
        }
        private static final long MILLIS_PER_SECOND = 1000;
        private static final long SECONDS_PER_MINUTE = 60;
        private static final long MINUTES_PER_HOUR = 60;
        private static final long HOURS_PER_DAY = 24;
        private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE;
        private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * MINUTES_PER_HOUR;
        private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * HOURS_PER_DAY;

        private String formatTimestamp(long m) {
            m = m % MILLIS_PER_DAY; // since midnight

            long hour = m / MILLIS_PER_HOUR;
            m = m % MILLIS_PER_HOUR; //since the hour
            long minute = m / MILLIS_PER_MINUTE;
            m = m % MILLIS_PER_MINUTE; //since the minute
            long second = m / MILLIS_PER_SECOND;
            long millis = m % MILLIS_PER_SECOND;

            StringBuilder sb = new StringBuilder(13);
            sb.append(twoDigitDecimal((int) hour));
            sb.append(':');
            sb.append(twoDigitDecimal((int) minute));
            sb.append(':');
            sb.append(twoDigitDecimal((int) second));
            sb.append('.');
            sb.append(threeDigitDecimal((int) millis));
            return sb.toString();
        }

        private String twoDigitDecimal(int num) {
            if (num < 10)
                return "0" + num;
            return Integer.toString(num);
        }

        private String threeDigitDecimal(int num) {
            if (num < 10)
                return "00" + num;
            if (num < 100)
                return "0" + num;
            return Integer.toString(num);
        }
    }

    @Override
    public synchronized void dump(String fileName) {
        stop();
        fileName = fileName.replace("~", System.getProperty("user.home"));
        fileName += ".gz";
        System.err.println("DUMPING FLIGHT LOG TO " + fileName + "...");
        System.err.println("AVAILABLE RECORDERS");
        System.err.println("====================");
        for (Map.Entry<Thread, ThreadRecorder> entry : recorders.entrySet())
            System.err.println("THREAD " + entry.getKey() + " TOTAL RECORDED: " + entry.getValue().getTotalRecs() + " AVAILABLE: " + entry.getValue().numOfElements());

        try {
            FileOutputStream fos = null;
            GZIPOutputStream gos = null;
            PrintStream ps = null;
            try {
                fos = new FileOutputStream(fileName);
                gos = new GZIPOutputStream(fos);
                ps = new PrintStream(gos);
                dump(ps);
            } finally {
                if (ps != null)
                    ps.close();
                if (gos != null)
                    gos.close();
                if (fos != null)
                    fos.close();
            }
            System.err.println("DUMPED FLIGHT LOG TO " + fileName);
        } catch (Exception ex) {
            System.err.println("EXCEPTION WHILE DUMPING FLIGHT LOG TO " + fileName);
            ex.printStackTrace();
        }
    }

    public synchronized void dump(PrintStream ps) {
        ps.println("============================");
        ps.println("=== FLIGHT RECORDER DUMP ===");
        ps.println("============================");
        ps.println();
        ps.println("AVAILABLE RECORDERS");
        ps.println("====================");
        for (Map.Entry<Thread, ThreadRecorder> entry : recorders.entrySet())
            ps.println("THREAD " + entry.getKey() + " TOTAL RECORDED: " + entry.getValue().getTotalRecs() + " AVAILABLE: " + entry.getValue().numOfElements());
        ps.println();
        ps.println("FLIGHT LOG");
        ps.println("====================");
        ps.println();
        for (Record record : getRecords())
            ps.println(record);
        ps.println();
        ps.println("NO MORE RECORDS");
        ps.println("====================");
    }
}
