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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Copies fields from an instance of a previous version of a class to the current version
 * @author pron
 */
class InstanceUpgrader {
    private final Class<?> toClass;
    private final Map<FieldDesc, Field> fields;
    private final ConcurrentMap<Class, Copier> copiers;
    private final Constructor<?> ctor;

    public InstanceUpgrader(Class<?> toClass) {
        this.toClass = toClass;
        this.copiers = new MapMaker().weakKeys().makeMap();
        this.fields = ImmutableMap.copyOf(getFields(toClass, new HashMap<FieldDesc, Field>()));
        for (Field f : fields.values())
            f.setAccessible(true);

        Constructor<?> c;
        try {
            c = toClass.getDeclaredConstructor();
            c.setAccessible(true);
        } catch (NoSuchMethodException e) {
            c = null;
        }
        this.ctor = c;
    }

    public void copy(Object from, Object to) {
        assert toClass.isInstance(to);
        getCopier(from.getClass()).copy(from, to);
    }

    public void copy(Object from) {
        if (ctor == null)
            throw new RuntimeException("Class " + toClass.getName() + " does not have a no-arg constructor.");
        try {
            getCopier(from.getClass()).copy(from, ctor.newInstance());
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

        Copier(Class<?> fromClass) {
            Map<FieldDesc, Field> fs = getFields(toClass, new HashMap<FieldDesc, Field>());

            ArrayList<Field> ffs = new ArrayList<>();
            ArrayList<Field> tfs = new ArrayList<>();
            for (Map.Entry<FieldDesc, Field> e : fs.entrySet()) {
                Field ff = e.getValue();
                Field tf = fields.get(e.getKey());
                if (tf != null && ff.getType() == tf.getType()) {
                    ffs.add(ff);
                    tfs.add(tf);
                }
            }
            this.fromFields = ffs.toArray(new Field[ffs.size()]);
            this.toFields = tfs.toArray(new Field[tfs.size()]);

            for (Field f : fromFields)
                f.setAccessible(true);
        }

        void copy(Object from, Object to) {
            try {
                for (int i = 0; i < fromFields.length; i++)
                    toFields[i].set(to, fromFields[i].get(from));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static Map<FieldDesc, Field> getFields(Class<?> clazz, Map<FieldDesc, Field> fields) {
        if (clazz == null
                || clazz.getPackage().getName().startsWith("co.paralleluniverse"))
            return fields;
        for (Field f : clazz.getDeclaredFields())
            fields.put(new FieldDesc(f), f);

        return getFields(clazz.getSuperclass(), fields);
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
}
