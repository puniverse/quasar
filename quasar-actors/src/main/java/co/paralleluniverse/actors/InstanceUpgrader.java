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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.util.Exceptions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.ReflectionFactory;

/**
 * Copies fields from an instance of a previous version of a class to the current version
 *
 * @author pron
 */
class InstanceUpgrader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceUpgrader.class);
    private static final Object reflFactory;
    static final ClassValue<InstanceUpgrader<?>> instanceUpgrader = new ClassValue<InstanceUpgrader<?>>() {
        @Override
        protected InstanceUpgrader<?> computeValue(Class<?> type) {
            return new InstanceUpgrader(type);
        }
    };

    static {
        Object rf = null;
        try {
            rf = ReflectionFactory.getReflectionFactory();
        } catch (Throwable t) {
        }
        reflFactory = rf;
    }

    public static <T> InstanceUpgrader<T> get(Class<T> clazz) {
        return (InstanceUpgrader<T>) instanceUpgrader.get(clazz);
    }
    private final Class<T> toClass;
    private final Map<FieldDesc, FieldInfo> fields;
    private final ConcurrentMap<Class, Copier> copiers;
    private final Constructor<T> ctor;

    public InstanceUpgrader(Class<T> toClass) {
        this.toClass = toClass;
        this.copiers = new MapMaker().weakKeys().makeMap();
        Map<FieldDesc, Field> fs = getInstanceFields(toClass, new HashMap<FieldDesc, Field>());
        ImmutableMap.Builder<FieldDesc, FieldInfo> builder = ImmutableMap.builder();
        for (Map.Entry<FieldDesc, Field> entry : fs.entrySet()) {
            Field f = entry.getValue();
            f.setAccessible(true);

            Constructor innerClassCtor = null;
            if (Objects.equals(f.getType().getEnclosingClass(), toClass)) {
                try {
                    innerClassCtor = f.getType().getConstructor(toClass);
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(e);
                }
            }
            builder.put(entry.getKey(), new FieldInfo(f, innerClassCtor));
        }
        this.fields = builder.build();
        this.ctor = getNoArgConstructor(toClass);
    }

    private static <T> Constructor<T> getNoArgConstructor(Class<T> cl) {
        if (reflFactory == null)
            return getNoArgConstructor1(cl);
        else
            return getNoArgConstructor2(cl);
    }

    private static <T> Constructor<T> getNoArgConstructor1(Class<T> cl) {
        try {
            Constructor cons = cl.getDeclaredConstructor();
            cons.setAccessible(true);
            return cons;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static <T> Constructor<T> getNoArgConstructor2(Class<T> cl) {
        Class<?> initCl = Actor.class.isAssignableFrom(cl) ? Actor.class : Object.class;
        try {
            Constructor cons = initCl.getDeclaredConstructor();
//                int mods = cons.getModifiers();
//                if ((mods & Modifier.PRIVATE) != 0
//                        || ((mods & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0
//                        && !packageEquals(cl, initCl))) {
//                    return null;
//                }
            cons = ((ReflectionFactory) reflFactory).newConstructorForSerialization(cl, cons);
            cons.setAccessible(true);
            return cons;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

//    private static boolean packageEquals(Class<?> cl1, Class<?> cl2) {
//        return cl1.getPackage().getName().equals(cl2.getPackage().getName()); // && cl1.getClassLoader() == cl2.getClassLoader();
//    }
    public Object copy(Object from, Object to) {
        assert toClass.isInstance(to);
        return getCopier((Class<T>) from.getClass()).copy(from, to);
    }

    public Object copy(Object from) {
        return getCopier((Class<T>) from.getClass()).copy(from);
    }

    private Copier<T> getCopier(Class<?> fromClass) {
        Copier copier = copiers.get(fromClass);
        if (copier == null) {
            copier = new Copier(fromClass);
            Copier temp = copiers.putIfAbsent(fromClass, copier);
            if (temp != null)
                copier = temp;
        }
        return copier;
    }

    private class Copier<T> {
        private final Field[] fromFields;
        private final Field[] toFields;
        private final Constructor[] innerClassConstructor;
        private final Copier[] fieldCopier;

        Copier(Class<T> fromClass) {
            if (!fromClass.getName().equals(toClass.getName()))
                throw new IllegalArgumentException("'fromClass' " + fromClass.getName() + " is not a version of 'toClass' " + toClass.getName());

            Map<FieldDesc, Field> fs = getInstanceFields(fromClass, new HashMap<FieldDesc, Field>());

            ArrayList<Field> ffs = new ArrayList<>();
            ArrayList<Field> tfs = new ArrayList<>();
            ArrayList<Constructor> ics = new ArrayList<>();
            ArrayList<Copier> fcs = new ArrayList<>();
            for (Map.Entry<FieldDesc, Field> e : fs.entrySet()) {
                Field ff = e.getValue();
                FieldInfo tfi = fields.get(e.getKey());
                Field tf = tfi != null ? tfi.field : null;

                if (tf != null) {
                    boolean assignable = false;
                    Constructor innerClassCtor = null;
                    Copier fc = null;

                    if (Objects.equals(ff.getType().getEnclosingClass(), fromClass)
                            && Objects.equals(tf.getType().getEnclosingClass(), toClass)) {
                        innerClassCtor = tfi.innerClassCtor;
                    } else if (tf.getType().isAssignableFrom(ff.getType())) {
                        assignable = true;
                    } else if (tf.getType().getName().equals(ff.getType().getName())) {
                        fc = instanceUpgrader.get(tf.getType()).getCopier(ff.getType());
                    }
                    if (assignable || innerClassCtor != null || fc != null) {
                        ffs.add(ff);
                        tfs.add(tf);
                        fcs.add(fc);
                        ics.add(innerClassCtor);
                    }
                }
            }
            this.fromFields = ffs.toArray(new Field[ffs.size()]);
            this.toFields = tfs.toArray(new Field[tfs.size()]);
            this.fieldCopier = fcs.toArray(new Copier[fcs.size()]);
            this.innerClassConstructor = ics.toArray(new Constructor[ics.size()]);

            for (Field f : fromFields)
                f.setAccessible(true);
        }

        Object copy(Object from, Object to) {
            try {
                for (int i = 0; i < fromFields.length; i++) {
                    final Object fromFieldValue = fromFields[i].get(from);
                    final Object toFieldValue;
                    if (innerClassConstructor[i] != null)
                        toFieldValue = innerClassConstructor[i].newInstance(to);
                    else if (fieldCopier[i] != null)
                        toFieldValue = fieldCopier[i].copy(fromFieldValue);
                    else
                        toFieldValue = fromFieldValue;

                    //LOG.debug("== {} <- {}: {} ({})", toFields[i], fromFields[i], toFieldValue, fromFieldValue);
                    toFields[i].set(to, toFieldValue);
                }
                return to;
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }

        Object copy(Object from) {
            if (ctor == null)
                throw new RuntimeException("Class " + toClass.getName()
                        + " in module " + (toClass.getClassLoader() instanceof ActorModule ? toClass.getClassLoader() : null)
                        + " does not have a no-arg constructor.");
            try {
                Object to = ctor.newInstance();
                return copy(from, to);
            } catch (InstantiationException | InvocationTargetException ex) {
                throw Exceptions.rethrow(ex.getCause());
            } catch (IllegalAccessException ex) {
                throw new AssertionError(ex);
            }
        }
    }

    private static Map<FieldDesc, Field> getInstanceFields(Class<?> clazz, Map<FieldDesc, Field> fields) {
        if (clazz == null)
            return fields;
        for (Field f : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()))
                fields.put(new FieldDesc(f), f);
        }

        return getInstanceFields(clazz.getSuperclass(), fields);
    }

    private static class FieldDesc {
        final String declaringClass;
        final String name;

        FieldDesc(Field field) {
            this(field.getDeclaringClass().getName(), field.getName());
        }

        FieldDesc(String declaringClass, String name) {
            this.declaringClass = declaringClass;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FieldDesc))
                return false;
            final FieldDesc other = (FieldDesc) obj;
            return this.declaringClass.equals(other.declaringClass) && this.name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return declaringClass.hashCode() ^ name.hashCode();
        }
    }

    private static class FieldInfo {
        final Field field;
        final Constructor innerClassCtor;

        public FieldInfo(Field field, Constructor innerClassCtor) {
            this.field = field;
            this.innerClassCtor = innerClassCtor;
        }
    }
}
