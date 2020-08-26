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
/**
 * <h2>Records - managed shared mutable state</h2>
 *
 * Here's an example record type definition:
 *
 * <pre>{@code
 * class A {
 *     public static final RecordType<A> aType = RecordType.newType(A.class);
 *     public static final IntField<A> $id = stateType.intField("id");
 *     public static final DoubleField<A> $foo = stateType.doubleField("id", Field.TRANSIENT);
 *     public static final ObjectField<A, String> $name = stateType.objectField("name", String.class);
 *     public static final ObjectField<A, List<String>> $emails = stateType.objectField("emails", new TypeToken<List<String>() {});
 * }
 * }</pre>
 * 
 * {@code A} is the type's <i>identifier class</i>. The fields are instances of {@link co.paralleluniverse.data.record.Field Field} and are, by convention, 
 * given identifiers that begin with a {@code $} to make it clear that they identify fields rather than values.
 * <br/>
 * A new record is instantiated by calling one of {@link co.paralleluniverse.data.record.RecordType RecordType}'s {@code newInstance} methods.
 * 
 */
package co.paralleluniverse.data.record;
