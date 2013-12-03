---
layout: default
title: Records
weight: 3
---

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

Just as actors expose their operations through a simple, standard interface that allows (pretty-much) only to send it messages, so should state be exposed through a simple, standard, interface. A record, then, is such a data-access interface akin to a map. It basically has get(field) and set(field, value) methods, only it preserves the type information of its field to provide type safety.

Records provide similar functionality to plain Java objects with public fields, but, unlike plain objects, they allow us to inject cross-cutting concerns on field-get and field-set operations, like restricting access to specific strands, or making sure that state is mutated (or read) only inside transactions.

Just as objects are instances of a classe, so too are records instances of a *record type*, represented by the [`RecordType`]({{javadoc}}/data/record/RecordType.html) class. A new record type must be declared as a static member of a class. The class must only include the definition of a single record type, and this class is called the type's *identifier class*, because it is used only to uniquely identify the record type (only its name is used internally).

Here's an example record type definition:

~~~ java
class A {
	public static final RecordType<A> aType = RecordType.newType(A.class);
	public static final IntField<A> $id = stateType.intField("id");
	public static final DoubleField<A> $foo = stateType.doubleField("id", Field.TRANSIENT);
	public static final ObjectField<A, String> $name = stateType.objectField("name", String.class);
	public static final ObjectField<A, List<String>> $emails = stateType.objectField("emails", new TypeToken<List<String>() {});
}
~~~

`A` is the type's identifier class. The fields are instances of [`Field`]({{javadoc}}/data/record/Field.html) and are, by convention, given identifiers that begin with a `$` to make it clear that they identify fields rather than values.

Record types, like classes, can extend a parent record type by providing the super-type to `RecordType.newType` or to `RecordType`'s constructor.

A new record is instantiated by calling one of `RecordType`'s `newInstance` methods. Please consult the [Javadoc]({{javadoc}}/data/record/RecordType.html) for details.

So, instead of writing `obj.getX()`, or `obj.x` we write `obj.get($x)`. What does this get us other than re-inventing what is a basic Java functionality, minus the some type safety? Like actors, records give up some type safety (we preserve the type of the `x` field, but the compiler can’t tell us whether obj even has an `x` field; similarly, if we send message `m` to actor `a`, the compiler can’t know whether `a` supports an `m` operation), they do so at well-defined interface points between separate software components. What we gain is loose coupling. For example, among other things, we gain the ability to swap the implementation of the record or actor at runtime for maintenance (hot code-swapping).

We gain other things by limiting component interaction to the narrow interfaces of actors and records, and that is the ability to insert cross-cutting concerns. For example, what happens if a method that consumes a resource is called too often? We need to explicitely insert load-handling code into the method. But if we communicate with the component through an actor interface, we can implement a general policy of handling too many messages that are thrown at any actor. Similarly with records. Parallel Universe's database, SpaceBase, uses records to restrict read and writes of shared state to well-defined transactions. Attempts to read or write state outside a transaction will throw a runtime exception.

Because records are intended to control mutability, an `ObjectField` should never reference a mutable object. `RecordType` will perform a very simple test on an `ObjectField` type and output a warning to the console if the class appears mutable. Conclusively determining whether a class is mutable or not is extremely difficult, so the test is a very simple one: it will warn if the class has public non-static, non-final fields, or if it has public methods whose name begins with "set".

