---
layout: default
title: Getting Started
weight: 1
---

## System requirements

Java 7 and is required to run Quasar.

## Using Leiningen {#lein}

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

Then, the following must be added to the java command line (or use your favorite build tool to add this as a JVM argument):

~~~ sh
-javaagent:path-to-quasar-jar.jar
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
