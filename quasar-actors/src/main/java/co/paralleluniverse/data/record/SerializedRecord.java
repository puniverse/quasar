/*
 * Copyright (c) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.data.record;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 *
 * @author pron
 */
final class SerializedRecord<R> implements Serializable {
    public static final long serialVersionUID = 8978406234220L;
    
    private Record<R> r;

    public SerializedRecord(Record<R> r) {
        this.r = r;
    }

    public SerializedRecord() {
        this.r = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(r.type().getName());
        out.writeInt(r.fields().size());
        r.write(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        final String typeName = in.readUTF();
        final int numFields = in.readInt();
        RecordType<R> type = (RecordType<R>) RecordType.forClass(Class.forName(typeName));
        r = type.newInstance();
        r.read(in, numFields);
    }

    private Object readResolve() throws ObjectStreamException {
        return r;
    }
//    private void readObjectNoData() throws ObjectStreamException {
//        throw new ObjectStreamException();
//    }
//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        out.writeUTF(r.type().getName());
//        out.writeInt(r.fields().size());
//        r.write(out);
//    }
//
//    @Override
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        final String typeName = in.readUTF();
//        final int numFields = in.readInt();
//        RecordType<R> type = (RecordType<R>) RecordType.forClass(Class.forName(typeName));
//        r = type.newInstance();
//        r.read(in, numFields);
//    }
}
