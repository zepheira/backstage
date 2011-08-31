Backstage
=========

This is the source for Backstage, originally from the [SIMILE SVN
repository][1].  We expect to see this satisfy the need for a
server-side backend working with [Exhibit 3.0][2].

Usage
-----

You will need a JVM installed with appropriate enviornment settings
(`JAVA_HOME`, etc) along with [Maven 2.x][4].  You will also need
[SIMILE Butterfly][3] installed in a peer directory to Backstage, like:

```
src/
  backstage/
  butterfly/
```

Butterfly can be checked out from Google Code repository with SVN.  The
Google Code version supercedes the SIMILE repository version, but the
GC version does not currently utilize Maven.  In order to run Backstage
with the GC Butterfly, you must copy all of its direct dependency JARs
out of the Maven repository into its own MOD-INF/lib/ directory.

Butterfly will be augmented in the near future so this rather annoying
step can be obviated.  For now, you can copy them into place with a
one-line UNIX command:

```
% mvn dependency:build-classpath | egrep -v '\[(INFO|WARNING)\]' | tr ':' '\n' | xargs -I% cp % modules/backstage/MOD-INF/lib/

```

Not every one copied over is a direct dependency; the indirect ones may
produce conflicts with Butterfly's classloading and should be removed.
The list of vital Backstage module JARs (anything not in Babel, Aduna, or
Sesame, other than commons-codec, should be removed):

```
aduna-commons-collections-2.8.0.jar
aduna-commons-concurrent-2.7.0.jar
aduna-commons-i18n-1.4.0.jar
aduna-commons-io-2.10.0.jar
aduna-commons-iteration-2.10.0.jar
aduna-commons-lang-2.9.0.jar
aduna-commons-net-2.7.0.jar
aduna-commons-text-2.7.0.jar
aduna-commons-xml-2.7.0.jar
babel-bibtex-converter-1.0.jar
babel-engine-1.0.jar
babel-exhibit-converter-1.0.jar
babel-interfaces-1.0.jar
commons-codec-1.5.jar
sesame-model-2.4.2.jar
sesame-query-2.4.2.jar
sesame-queryalgebra-evaluation-2.4.2.jar
sesame-queryalgebra-model-2.4.2.jar
sesame-queryparser-api-2.4.2.jar
sesame-queryparser-serql-2.4.2.jar
sesame-repository-api-2.4.2.jar
sesame-repository-sail-2.4.2.jar
sesame-rio-api-2.4.2.jar
sesame-rio-n3-2.4.2.jar
sesame-rio-ntriples-2.4.2.jar
sesame-rio-rdfxml-2.4.2.jar
sesame-rio-trix-2.4.2.jar
sesame-rio-turtle-2.4.2.jar
sesame-sail-api-2.4.2.jar
sesame-sail-inferencer-2.4.2.jar
sesame-sail-memory-2.4.2.jar
sesame-sail-nativerdf-2.4.2.jar
whirlycache-1.0.1.jar
```

[1]: http://simile.mit.edu/repository/backstage/trunk/
[2]: https://github.com/zepheira/exhibit3/
[3]: https://code.google.com/p/simile-butterfly/
[4]: http://maven.apache.org/
