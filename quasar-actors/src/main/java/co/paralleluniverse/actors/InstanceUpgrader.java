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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies fields from an instance of a previous version of a class to the current version
 *
 * @author pron
 */
class InstanceUpgrader {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceUpgrader.class);
    private final Class<?> toClass;
    private final Map<FieldDesc, FieldInfo> fields;
    private final ConcurrentMap<Class, Copier> copiers;
    private final Constructor<?> ctor;

    public InstanceUpgrader(Class<?> toClass) {
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


        Constructor<?> c;
        try {
            c = toClass.getDeclaredConstructor();
            c.setAccessible(true);
        } catch (NoSuchMethodException e) {
            c = null;
        }
        this.ctor = c;
    }

    public Object copy(Object from, Object to) {
        assert toClass.isInstance(to);
        return getCopier(from.getClass()).copy(from, to);
    }

    public Object copy(Object from) {
        if (ctor == null)
            throw new RuntimeException("Class " + toClass.getName()
                    + " in module " + (toClass.getClassLoader() instanceof ActorModule ? toClass.getClassLoader() : null)
                    + " does not have a no-arg constructor.");
        try {
            Object to = ctor.newInstance();
            return getCopier(from.getClass()).copy(from, to);
        } catch (InstantiationException | InvocationTargetException ex) {
            throw Exceptions.rethrow(ex.getCause());
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    private Copier getCopier(Class<?> fromClass) {
        Copier copier = copiers.get(fromClass);
        if (copier == null) {
            copier = new Copier(fromClass);
            Copier temp = copiers.putIfAbsent(fromClass, copier);
            if (temp != null)
                copier = temp;
        }
        return copier;
    }

    private class Copier {
        private final Field[] fromFields;
        private final Field[] toFields;
        private final Constructor[] innerClassConstructor;

        Copier(Class<?> fromClass) {
            if (!fromClass.getName().equals(toClass.getName()))
                throw new IllegalArgumentException("'fromClass' " + fromClass.getName() + " is not a version of 'toClass' " + toClass.getName());

            Map<FieldDesc, Field> fs = getInstanceFields(toClass, new HashMap<FieldDesc, Field>());

            ArrayList<Field> ffs = new ArrayList<>();
            ArrayList<Field> tfs = new ArrayList<>();
            ArrayList<Constructor> ics = new ArrayList<>();
            for (Map.Entry<FieldDesc, Field> e : fs.entrySet()) {
                Field ff = e.getValue();
                FieldInfo tfi = fields.get(e.getKey());
                Field tf = tfi != null ? tfi.field : null;

                if (tf != null) {
                    Constructor innerClassCtor = null;
                    if (Objects.equals(ff.getType().getEnclosingClass(), fromClass)
                            && Objects.equals(tf.getType().getEnclosingClass(), toClass)) {
                        innerClassCtor = tfi.innerClassCtor;
                    }
                    if (innerClassCtor != null || tf.getType().isAssignableFrom(ff.getType())) {
                        ffs.add(ff);
                        tfs.add(tf);
                        ics.add(innerClassCtor);
                    }
                }
            }
            this.fromFields = ffs.toArray(new Field[ffs.size()]);
            this.toFields = tfs.toArray(new Field[tfs.size()]);
            this.innerClassConstructor = ics.toArray(new Constructor[ics.size()]);

            for (Field f : fromFields)
                f.setAccessible(true);
        }

        Object copy(Object from, Object to) {
            try {
                for (int i = 0; i < fromFields.length; i++) {
                    if (innerClassConstructor[i] != null)
                        toFields[i].set(to, innerClassConstructor[i].newInstance(to));
                    else {
                        LOG.debug("== " + toFields[i] + " <- " + fromFields[i].get(from));
                        toFields[i].set(to, fromFields[i].get(from));
                    }
                }
                return to;
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }
    }
    private static final List<String> infrastructure = Arrays.asList(new String[]{
                "co.paralleluniverse.strands",
                "co.paralleluniverse.fibers",
                "co.paralleluniverse.actors",
    });

    private static boolean startsWithAnyOf(String str, Collection<String> prefixes) {
        for (String prefix : prefixes) {
            if (str.startsWith(prefix))
                return true;
        }
        return false;
    }

    private static Map<FieldDesc, Field> getInstanceFields(Class<?> clazz, Map<FieldDesc, Field> fields) {
        if (clazz == null) //  || startsWithAnyOf(clazz.getPackage().getName(), infrastructure))
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
