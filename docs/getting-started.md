---
layout: default
title: Getting Started
weight: 1
---

## System requirements

Java 7 and Clojure 1.5 are required to run Pulsar.

## Using Leiningen {#lein}

Add the following dependency to [Leiningen](http://github.com/technomancy/leiningen/)'s project.clj:

~~~ clojure
[co.paralleluniverse/pulsar "0.2.0"]
~~~

Then, the following must be added to the project.clj file:

~~~ clojure
:java-agents [[co.paralleluniverse/quasar-core "0.2.0"]]
~~~

or, add the following to the java command line:

~~~ sh
-javaagent:path-to-quasar-jar.jar
~~~


[Leiningen]: http://github.com/technomancy/leiningen/

## Building Pulsar {#build}

Clone the repository:

    git clone git://github.com/puniverse/pulsar.git pulsar

and run:

    lein midje

To build the documentation, you need to have [Jekyll] installed. Then run:

    jekyll build

To generate the API documentation run

    lein doc


You can run the examples like this:


    lein -o run -m co.paralleluniverse.pulsar.examples.pingpong


For benchmarks, you should use `lein trampoline`, like so:


    lein trampoline run -m co.paralleluniverse.pulsar.examples.ring-benchmark 1000 1000


[Jekyll]: http://jekyllrb.com/


{% comment %}
**Note**: Blah blah blah 
{:.centered .alert .alert-info}

**Note**: Blah blah blah 
{:.alert}

**Note**: Blah blah blah 
{:.alert .alert-error}
{% endcomment %}
