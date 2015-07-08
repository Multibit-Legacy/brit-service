## MultiBit BRIT Service

Build status: [![Build Status](https://travis-ci.org/bitcoin-solutions/brit-service.png?branch=develop)](https://travis-ci.org/bitcoin-solutions/brit-service)

This repo contains the source for the MultiBit BRIT service.

From a technical point of view this project uses

* Java - Primary language of the app
* [Maven](http://maven.apache.org/) - Build system
* [Dropwizard](http://dropwizard.io) - Self-contained web server

## Read more about BRIT

We have provided an [overview of BRIT](src/main/doc/Overview-of-BRIT.md) and detailed instructions for each of
the actors involved:

* [Creating Redeemer wallets](src/main/doc/Redeemer-1-Creating-Bitcoin-wallets.md)
* [Setting up a Matcher (BRIT server)](src/main/doc/Matcher-1-Setting-up.md)
* [Setting up a Payer](src/main/doc/Payer-1-Setting-up.md)

The rest of this document provides the detailed instructions for running up a BRIT server.

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

At present it is necessary to checkout [multibit-hd](https://github.com/bitcoin-solutions/multibit-hd/) and
build it manually. You will need to use the HEAD of the `develop` branch.

    mvn clean install

Later this will be available in a host repository (such as Maven Central) which will simplify the build process.

### BRIT server config

The BRIT server uses a PGP key to authenticate the requests sent to it.

This is in a PGP secret keyring stored exactly here: `/var/brit/matcher/gpg/secring.gpg`

You will need to open up the permissions on the folders in `/var/brit` and its subdirectories using:

    sudo chmod a+wx -R /var/brit

To get a developer environment up and running just copy these values from `src/test/resources/matcher` into the
external location.

### Inside an IDE

Import the project as a Maven project in the usual manner.

To start the project you just need to execute `BritService.main()` as a Java application. You'll need a runtime configuration
that passes in `server config.yml` as the Program Arguments.

Open a browser to [http://localhost:9090/brit/public-key](http://localhost:9090/brit/public-key) and you should see the BRIT server
public key.

At this stage you can perform most development tasks and you won't be prompted for a password.

## Running BRIT server in Production

To run up BRIT server for real you need to run it outside of an IDE which introduces some security issues. Security Providers such
as Bouncy Castle can only be loaded from a trusted source. In the case of a JAR this means that it must be signed with a certificate
that has in turn been signed by one of the trusted Certificate Authorities (CAs) in the `cacerts` file of the JRE.

Fortunately the Bouncy Castle team have done this so the `bcprov-jdk16-1.46.jar` must be external to the server JAR.

This changes the  launch command line from a standard Dropwizard as follows:

    cd <project root>
    mvn clean install
    java -cp "bcprov-jdk16-1.46.jar:target/brit-service-<version>.jar" org.multibit.hd.brit.rest.BritService server config.yml

where `<project root>` is the root directory of the project as checked out through git and `<version>` is the version
as found in `pom.xml` (e.g. "develop-SNAPSHOT" or "1.0.0") but you'll see a `.jar` in the `target` directory so it'll be obvious.

The Bouncy Castle security provider library should be in the project root for development.

On startup you will need to provide the passphrase for the Matcher key store. It is not persisted anywhere.

All commands will work on *nix without modification, use \ instead of / for Windows.

## Test the BRIT server using a browser REST plugin

First open a browser to [http://localhost:9090/brit/public-key](http://localhost:9090/brit/public-key) and you should see the BRIT server
public key. Note it is port 9090 not the usual 8080.

If you are running Chrome and have the excellent Advanced REST Client extension installed then you can build a POST request for the
development environment as follows:

    Host: http://localhost:9090/brit
    Content-Type: text/plain
    Accept: application/octet-stream

    -----BEGIN PGP MESSAGE-----
    Version: BCPG v1.46

    hQEMA+aIld5YYUzuAQf9GkIWCi7FKON9JzdRzpWurjCTiqEizTxxL+Wu67D5eTMD
    MKm1Cz4pGjq5G9j0rtxBZCn7ua/qt6QWBlPFuYQWdbAN2gsLVUgcejHMjD2MCfZc
    eAAAi4moOZE4r22hKKIpvaj/4dMp8G7pBsHIKmMAJCWnUaPFB/FQJx6KQ4i8Hh+W
    OvE0Fi2CHNLf9zELSMN3IZT3lueuZzxmeg2VTNB6H3dVRvp+HiKTlJ4Mrz5iXx6s
    lh225PsprHWWY7sY74820sFjcrC3r7ITRmBHVk3uAUvlhLcE2Kfnvcsks/lylLSX
    Nqm8p2KGiji9FALeRbjEzNAZ1VNY9PMeSbbTkTg+YNKIAZwnU0uKwf78XbVLigNy
    YOSuwRiXU8HUIfe6hViawYvlAD/HsgIGi/5MMpcYu1Ehahjz4p4VLYJ37lHvMnHd
    d/0IjDb/jb1HYXqUbRyJeAlU89TMJMOxL7PnYvAnGZPZvb7wQMcf4WjvbjqIDJ+U
    Q5zVwa4UtipOlo7ItzOfzRTW5RHiu56ZIg==
    =twKa
    -----END PGP MESSAGE-----

If all goes well the response will be a `201_CREATED` with some gibberish to decrypt. The client and server are in synch.

A `400_BAD_REQUEST` indicates that the BRIT server is not able to decrypt the PayerRequest.

### Where does the ASCII art come from?

The ASCII art for the startup banner was created using the online tool available at
[TAAG](http://patorjk.com/software/taag/#p=display&f=Slant&t=BRIT)
