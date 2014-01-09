/*
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
package co.paralleluniverse.common.benchmark;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author pron
 */
public class StripedLongTimeSeries {
    public static class Record {
        public final long timestamp;
        public final long value;

        private Record(long timestamp, long value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
    private final Striped<ThreadRecorder> rs;
    private final boolean autoTimestamp;
    private final long startWallTime;
    private final long startTimestamp;

    public StripedLongTimeSeries(final int size, boolean autoTimestamp) {
        this.rs = new Striped<ThreadRecorder>() {
            @Override
            protected ThreadRecorder newResource() {
                return new ThreadRecorder(size);
            }
        };

        this.autoTimestamp = autoTimestamp;
        this.startTimestamp = System.nanoTime();
        this.startWallTime = System.currentTimeMillis();
    }

    public void record(long val) {
        if(!autoTimestamp)
            throw new IllegalStateException("Must pass timestamp if autoTimestamp is set");
        rs.get().record(System.nanoTime(), val);
    }

    public void record(long timestamp, long val) {
        if(autoTimestamp)
            throw new IllegalStateException("Cannot pass timestamp if autoTimestamp is set");
        rs.get().record(timestamp, val);
    }

    public static class ThreadRecorder {
        private final long[] timestamps;
        private final long[] payloads;
        private long totalRecs;
        private int head; // points to earliest entry available for reading
        private int tail; // points to slot where next record will be written

        private ThreadRecorder(int size) {
            timestamps = new long[size];
            payloads = new long[size];
            head = 0;
            tail = 0;
            totalRecs = 0;
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

        public void record(long timestamp, long val) {
            totalRecs++;
            timestamps[tail] = timestamp;
            payloads[tail] = val;
            tail = next(tail);
            if (tail == head)
                head = next(head);
        }
    }

    public Iterable<Record> getRecords() {
        return new Iterable<Record>() {
            @Override
            public Iterator<Record> iterator() {
                return new Iterator<Record>() {
                    final int n = rs.size();
                    final ThreadRecorder[] trs = new ThreadRecorder[n];
                    final int[] is = new int[n];
                    final long[] ts = new long[n];
                    final long[] ps = new long[n];
                    int nextFr = -1;
                    long lastTimestamp;

                    {
                        int i = 0;
                        for (ThreadRecorder r : rs) {
                            trs[i] = r;
                            is[i] = -1;
                            readNext(i);
                            i++;
                        }
                        assert i == n;
                        nextFr = findMin();
                        lastTimestamp = -1;
                    }

                    private void readNext(int index) {
                        ThreadRecorder tr = trs[index];
                        int i = is[index];
                        if ((i < 0 && tr.numOfElements() == 0) || tr.isLast(i)) {
                            is[index] = -1;
                            ts[index] = Long.MAX_VALUE;
                            ps[index] = Long.MIN_VALUE;
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
                        final long time = autoTimestamp ? startWallTime + (ts[index] - startTimestamp) / 1000000 : ts[index];
                        return new Record(time, ps[index]);
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
}
