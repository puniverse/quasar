/*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.continuations.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * An extension to {@link BasicInterpreter} which collects the type of
 * objects and arrays.
 * 
 * @author Matthias Mann
 */
public class TypeInterpreter extends BasicInterpreter {

    private final MethodDatabase db;

    public TypeInterpreter(MethodDatabase db) {
        this.db = db;
    }

    @Override
    public BasicValue newValue(Type type) {
        if(type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        if(type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return new BasicValue(type);
        }
        return super.newValue(type);
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        if(insn.getOpcode() == Opcodes.NEW) {
            return new NewValue(Type.getObjectType(((TypeInsnNode) insn).desc), false, insn);
        }
        return super.newOperation(insn);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        if(insn.getOpcode() == Opcodes.DUP) {
            if(value instanceof NewValue) {
                NewValue newValue = (NewValue)value;
                if(!newValue.isDupped) {
                    return new NewValue(newValue.getType(), true, insn);
                }
            }
        }
        return super.copyOperation(insn, value);
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
        if(insn.getOpcode() == Opcodes.AALOAD) {
            Type t1 = value1.getType();
            if(t1 == null || t1.getSort() != Type.ARRAY) {
                throw new AnalyzerException(insn, "AALOAD needs an array as first parameter");
            }
            
            Type resultType = Type.getType(t1.getDescriptor().substring(1));
            return new BasicValue(resultType);
        }
        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public BasicValue merge(BasicValue v, BasicValue w) {
        if (!v.equals(w)) {
            if(v.isReference() && w.isReference()) {
                int dimensions = 0;
                Type typeV = v.getType();
                Type typeW = w.getType();
                if(typeV.getSort() != typeW.getSort()) {
                    db.log(LogLevel.DEBUG, "Array and none array type can't be merged: %s %s", v, w);
                    return BasicValue.UNINITIALIZED_VALUE;
                }
                if(typeW.getSort() == Type.ARRAY) {
                    dimensions = typeV.getDimensions();
                    if(dimensions != typeW.getDimensions()) {
                        db.log(LogLevel.DEBUG, "Arrays with different dimensions can't be merged: %s %s", v, w);
                        return BasicValue.UNINITIALIZED_VALUE;
                    }
                    typeV = typeV.getElementType();
                    typeW = typeW.getElementType();
                    if(typeV.getSort() != Type.OBJECT || typeW.getSort() != Type.OBJECT) {
                        db.log(LogLevel.DEBUG, "Arrays of different primitive type can't be merged: %s %s", v, w);
                        return BasicValue.UNINITIALIZED_VALUE;
                    }
                }
                String internalV = typeV.getInternalName();
                String internalW = typeW.getInternalName();
                if("null".equals(internalV)) {
                    return w;
                }
                if("null".equals(internalW)) {
                    return v;
                }
                String superClass = db.getCommonSuperClass(internalV, internalW);
                if(superClass == null) {
                    if(db.isException(internalW)) {
                        db.log(LogLevel.WARNING, "Could not determine super class for v=%s w=%s - decided to use exception %s", v, w, w);
                        return w;
                    }
                    if(db.isException(internalV)) {
                        db.log(LogLevel.WARNING, "Could not determine super class for v=%s w=%s - decided to use exception %s", v, w, v);
                        return v;
                    }
                    db.log(LogLevel.WARNING, "Could not determine super class for v=%s w=%s - decided to use java/lang/Object", v, w);
                    superClass = "java/lang/Object";
                }
                String typeDescriptor = makeTypeDescriptor(superClass, dimensions);
                db.log(LogLevel.INFO, "Common super class for v=%s w=%s is %s", v, w, typeDescriptor);
                return new BasicValue(Type.getType(typeDescriptor));
            }
            return BasicValue.UNINITIALIZED_VALUE;
        }
        return v;
    }

    private static String makeTypeDescriptor(String className, int dimensions) {
        int len = className.length();
        char[] tmp = new char[len + 2 + dimensions];
        for(int i=0 ; i<dimensions ; i++) {
            tmp[i] = '[';
        }
        tmp[dimensions] = 'L';
        className.getChars(0, len, tmp, dimensions+1);
        tmp[dimensions+1+len] = ';';
        return new String(tmp);
    }
}
