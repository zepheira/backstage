Backstage
=========

This is the source for Backstage, originally from the [SIMILE SVN
repository][1].  We expect to see this satisfy the need for a
server-side backend working with [Exhibit 3.0][2].

Usage
-----

You will need a JVM installed with appropriate enviornment settings
(`JAVA_HOME`, etc) along with [Maven 2.x][4]+.  You will also need
[Babel][5] as well as [SIMILE Butterfly][3].  Butterfly should be
installed in a peer directory to Backstage, like:

```
src/
  backstage/
  butterfly/
```

Butterfly can be checked out from Google Code repository with SVN.  The
Google Code version supercedes the SIMILE repository version

Installing Maven dependencies
-----------------------------

Run `mvn install` for Babel to install the dependencies Backstage
needs.  Babel is currently in the process of transitioning to Maven
Central and is not reliably available in other Maven repositories.

Run `ant build` for Butterfly.

Setting up Backstage
--------------------

Run `mvn package` for Backstage to compile its classes to the correct
location and copy its Maven dependencies into place within the module.
Butterfly is no longer Maven-run software, so its classloader must be
able to find classes within the Backstage module.

[1]: http://simile.mit.edu/repository/backstage/trunk/
[2]: https://github.com/zepheira/exhibit3/
[3]: https://code.google.com/p/simile-butterfly/
[4]: http://maven.apache.org/
[5]: https://github.com/zepheira/babel/
