---
layout: default
title: Getting Started
weight: 1
---

## System requirements

Java 7 and is required to run Quasar.

## Using Maven {#maven}

Add the following dependency to Maven:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-core</artifactId>
    <version>0.3.0</version>
</dependency>
~~~

To use actors, add:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-actors</artifactId>
    <version>0.3.0</version>
</dependency>
~~~

For clustering support, add:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-galaxy</artifactId>
    <version>0.3.0</version>
</dependency>
~~~

## Running the Instrumentation Java Agent

The following must be added to the java command line (or use your favorite build tool to add this as a JVM argument):

~~~ sh
-javaagent:path-to-quasar-jar.jar
~~~

### Specifying the Java Agent with Maven:

The best way to do this with Maven, as explained [here](http://stackoverflow.com/questions/14777909/specify-javaagent-argument-with-maven-exec-plugin), is:

First, setup the [maven-dependency-plugin](http://maven.apache.org/plugins/maven-dependency-plugin/) to always run the "properties" goal.

~~~ xml
<plugin>
    <artifactId>maven-dependency-plugin</artifactId>
    <version>2.5.1</version>
    <executions>
        <execution>
            <id>getClasspathFilenames</id>
            <goals>
                <goal>properties</goal>
            </goals>
        </execution>
     </executions>
</plugin>
~~~

Later on, use the property it sets [as documented here](http://maven.apache.org/plugins/maven-dependency-plugin/properties-mojo.html) with the form:

~~~
groupId:artifactId:type:[classifier]
~~~

like so

~~~ xml
<argument>-javaagent:${co.paralleluniverse:quasar-core:jar}</argument>
~~~

### Specifying the Java Agent with Gradle

The way to do this with Gradle is as follows. Add a `quasar` configuration to your `build.gradle` file:

~~~ groovy
configurations {
    // ...
    quasar
}
~~~

In your dependencies block, add (where `VERSION` stands for the Quasar version):

~~~ groovy
dependencies {
    // ....
    quasar  "co.paralleluniverse:quasar-core:VERSION"
}
~~~

Finally, in your `run` task (or any task of type `JavaExec` or `Test`), add the system property:

~~~ groovy
jvmArgs "-javaagent:${configurations.quasar.iterator().next()}"
~~~

## Building Quasar {#build}

Clone the repository:

    git clone git://github.com/puniverse/quasar.git quasar

and run:

    ./gradlew

Or, if you have gradle installed, run:

    gradle


{% comment %}
**Note**: Blah blah blah 
{:.centered .alert .alert-info}

**Note**: Blah blah blah 
{:.alert}

**Note**: Blah blah blah 
{:.alert .alert-error}
{% endcomment %}
