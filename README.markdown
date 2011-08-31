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

Butterfly can be checked out from the SIMILE repository with SVN.

Run `mvn install` for Babel to install the dependencies Backstage
needs.  Babel is currently in the process of transitioning to Maven
Central and is not reliably available in other Maven repositories.

[1]: http://simile.mit.edu/repository/backstage/trunk/
[2]: https://github.com/zepheira/exhibit3/
[3]: http://simile.mit.edu/repository/butterfly/trunk/
[4]: http://maven.apache.org/
[5]: https://github.com/zepheira/babel/
