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
/**
 * ## Records
 *
 * Here's an example record type definition:
 *
 * ```java
 * class A {
 *     public static final RecordType<A> aType = RecordType.newType(A.class);
 *     public static final IntField<A> $id = stateType.intField("id");
 *     public static final DoubleField<A> $foo = stateType.doubleField("id", Field.TRANSIENT);
 *     public static final ObjectField<A, String> $name = stateType.objectField("name", String.class);
 *     public static final ObjectField<A, List<String>> $emails = stateType.objectField("emails", new TypeToken<List<String>() {});
 * }
 * ```
 * 
 * `A` is the type's *identifier class*. The fields are, by convention, given identifiers that begin with a {@code \$} to make it clear
 * that they identify fields rather than values.
 */
package co.paralleluniverse.data.record;
