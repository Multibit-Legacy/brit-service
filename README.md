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

## Verify you have Maven 3+

Most IDEs (such as [Intellij Community Edition](http://www.jetbrains.com/idea/download/)) come with support for Maven built in,
but if not then you may need to [install it manually](http://maven.apache.org/download.cgi).

IDEs such as Eclipse may require the [m2eclipse plugin](http://www.sonatype.org/m2eclipse) to be configured.

To quickly check that you have Maven 3+ installed check on the command line:

    mvn --version

A quick way to install Maven on Mac is to use HomeBrew.

## Manually build and install MultiBit HD BRIT support library (mbhd-brit)

At present it is necessary to checkout [multibit-hd](https://github.com/bitcoin-solutions/multibit-hd/) and build it manually. You will need to
use the HEAD of the `develop` branch.

    mvn clean install

Later this will be available in a host repository (such as Maven Central) which will simplify the build process.

### BRIT server config

The BRIT server uses a PGP key to authenticate the requests sent to it.

This is in a PGP secret keyring stored exactly here: `/var/brit/matcher/gpg/secring.gpg`

You will need to open up the permissions on the folders in `/var/brit` and its subdirectories using:

    sudo chmod a+wx -R /var/brit

### Inside an IDE

Import the project as a Maven project in the usual manner.

To start the project you just need to execute `BritService.main()` as a Java application. You'll need a runtime configuration
that passes in `server brit-config.yml` as the Program Arguments.

Open a browser to [http://localhost:9090/brit/public-key](http://localhost:9090/brit/public-key) and you should see the BRIT server
public key.

At this stage you can perform most development tasks.

## Running BRIT server in Production

To run up BRIT server for real you need to run it outside of an IDE which introduces some security issues. Security Providers such
as Bouncy Castle can only be loaded from a trusted environment. In the case of a JAR this means that it must be signed with a certificate
that has in turn been signed by one of the trusted Certificate Authorities (CAs) in the `cacerts` file of the JRE.

To get this running with a self-signed certificate you need to jump through some key signing hoops as follows:

1) Create a key store for the self-signed certificate

    keytool -selfcert -alias signfiles -keystore examplestore

2) Verify the contents of the key store

    keytool -list -keystore examplestore

3) Generate a key for signing files and put it into the key store

    keytool -genkey -alias signfiles -keystore examplestore

4) Sign the JAR

    jarsigner -keystore examplestore -signedjar sCount.jar Count.jar signfiles

5)

### Build the JAR and sign it

All the JAR signing work is already in place in the `pom.xml` so assuming that you've prepared your key store correctly you can build
it as follows:

    cd <project root>
    mvn clean install
    java -jar target/brit-server-<version>.jar server brit-config.yml

where `<project root>` is the root directory of the project as checked out through git and `<version>` is the version
as found in `pom.xml` (e.g. "1.0.0") but you'll see a `.jar` in the `target` directory so it'll be obvious.

On startup you will need to provide the passphrase for the Matcher key store. It is not persisted anywhere.

All commands will work on *nix without modification, use \ instead of / for Windows.

Open a browser to [http://localhost:9090/brit/public-key](http://localhost:9090/brit/public-key) and you should see the BRIT server
public key. Note it is port 9090 not the usual 8080.

### Where does the ASCII art come from?

The ASCII art for the startup banner was created using the online tool available at
[TAAG](http://patorjk.com/software/taag/#p=display&f=Slant&t=BRIT%20Server)
