#!/bin/bash
echo Starting BRIT server with PID: $$;
echo Usage:
echo 1. At the password prompt, enter the password for the matcher secret key ring.
echo 2. Send the BRIT server to background with CTRL+Z
echo 3. Restart the stopped job in the background by typing 'bg'.
echo 4. Exit from shell 
echo 5. Verify BRIT server is still running by checking
echo ..   TEST: http://localhost:7070/brit/public-key   or 
echo ..   LIVE: http://multibit.org/brit/public-key
echo ..
echo TIP: You can find this process again by typing 'ps -A | grep brit'
java -cp "bcprov-jdk16-1.46.jar:target/brit-server-develop-SNAPSHOT.jar" org.multibit.hd.brit_server.BritService server brit-config.yml

