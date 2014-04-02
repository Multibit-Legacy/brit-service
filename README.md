## MultiBit BRIT Server

This repo contains the source for the MultiBit BRIT server.

From a technical point of view this project uses

* Java - Primary language of the app
* [Maven](http://maven.apache.org/) - Build system
* [Dropwizard](http://dropwizard.io) - Self-contained web server

## Branches

We follow the ["master-develop" branching strategy](http://nvie.com/posts/a-successful-git-branching-model/).

This means that the latest release is on the "master" branch (the default) and the latest release candidate is on the "develop" branch.
Any issues are addressed in feature branches from "develop" and merged in as required.

#### Verify you have Maven 3+

Most IDEs (such as [Intellij Community Edition](http://www.jetbrains.com/idea/download/)) come with support for Maven built in,
but if not then you may need to [install it manually](http://maven.apache.org/download.cgi).

IDEs such as Eclipse may require the [m2eclipse plugin](http://www.sonatype.org/m2eclipse) to be configured.

To quickly check that you have Maven 3+ installed check on the command line:
```
$ mvn --version
```

#### Manually build and install MultiBit HD BRIT

At present it is necessary to checkout [multibit-hd](https://github.com/bitcoin-solutions/multibit-hd/) and build it manually. You will need to
use the HEAD of the `develop` branch.
```
$ mvn clean install
```

### Inside an IDE

Import the project as a Maven project in the usual manner.

To start the project you just need to execute `BritService.main()` as a Java application. You'll need a runtime configuration
that passes in `server brit-config.yml` as the Program Arguments.

On startup you will need to provide the passphrase for the Matcher key store. It is not persisted anywhere.

Open a browser to [http://localhost:9090/brit/public-key](http://localhost:9090/brit/public-key) and you should see the BRIT server
public key.

### Outside of an IDE

Assuming that you've got Java and Maven installed you'll find it very straightforward to get the BRIT server running. Just clone
from GitHub and do the following:

```
cd <project root>
mvn clean install
java -jar target/brit-server-<version>.jar server brit-config.yml
```

where `<project root>` is the root directory of the project as checked out through git and `<version>` is the version
as found in `pom.xml` (e.g. "1.0.0") but you'll see a `.jar` in the `target` directory so it'll be obvious.

All commands will work on *nix without modification, use \ instead of / for Windows.

Open a browser to [http://localhost:9090/brit/public-key](http://localhost:9090/brit/public-key) and you should see the BRIT server
public key. Note it is port 9090 not the usual 8080.

### Where does the ASCII art come from?

The ASCII art for the startup banner was created using the online tool available at
[TAAG](http://patorjk.com/software/taag/#p=display&f=Slant&t=BRIT%20Server)
