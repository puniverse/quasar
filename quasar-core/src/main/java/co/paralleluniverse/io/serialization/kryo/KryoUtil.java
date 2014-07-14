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
package co.paralleluniverse.io.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

/**
 *
 * @author pron
 */
public final class KryoUtil {
    public static Kryo newKryo() {
        Kryo kryo = new ReplaceableObjectKryo();

        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
        registerCommonClasses(kryo);
        return kryo;
    }

    public static void registerCommonClasses(Kryo kryo) {
        kryo.register(boolean[].class);
        kryo.register(byte[].class);
        kryo.register(short[].class);
        kryo.register(char[].class);
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(long[].class);
        kryo.register(double[].class);
        kryo.register(String[].class);
        kryo.register(int[][].class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.LinkedList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.LinkedHashMap.class);
        kryo.register(java.util.TreeMap.class);
        kryo.register(java.util.EnumMap.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(java.util.TreeSet.class);
        kryo.register(java.util.EnumSet.class);

        kryo.register(java.util.Arrays.asList("").getClass(), new ArraysAsListSerializer());
        kryo.register(java.util.Collections.newSetFromMap(new java.util.HashMap()).getClass(), new CollectionsSetFromMapSerializer());
//        kryo.register(java.util.Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
//        kryo.register(java.util.Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
//        kryo.register(java.util.Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
//        kryo.register(java.util.Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
//        kryo.register(java.util.Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
//        kryo.register(java.util.Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
        kryo.register(java.util.GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(java.lang.reflect.InvocationHandler.class, new JdkProxySerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);
        kryo.addDefaultSerializer(Externalizable.class, new ExternalizableKryoSerializer());
        kryo.addDefaultSerializer(java.lang.ref.Reference.class, new ReferenceSerializer());
    }

    public static ObjectOutput asObjectOutput(Output output, Kryo kryo) {
        return new KryoObjectOutputStream(output, kryo);
    }

    public static ObjectInput asObjectInput(Input input, Kryo kryo) {
        return new KryoObjectInputStream(input, kryo);
    }

    private KryoUtil() {
    }
}
