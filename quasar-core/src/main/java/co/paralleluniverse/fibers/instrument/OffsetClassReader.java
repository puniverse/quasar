/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

/**
 * @author circlespainter
 */
class OffsetClassReader extends ClassReader {
    OffsetClassReader(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected Label readLabel(int i, Label[] labels) {
        final Label ret = super.readLabel(i, labels);
        ret.info = i;
        return ret;
    }
}