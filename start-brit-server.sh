#!/bin/bash
echo Starting BRIT server
java -cp "bcprov-jdk16-1.46.jar:target/brit-server-develop-SNAPSHOT.jar" org.multibit.hd.brit_server.BritService server brit-config.yml

