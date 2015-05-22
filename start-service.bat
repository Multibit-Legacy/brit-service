echo off
echo Starting BRIT service with PID: $$;
echo Usage:
echo 1. At the password prompt, enter the password for the secret key ring
echo 2. Send the service to background with CTRL+Z
echo 3. Restart the stopped job in the background by typing 'bg'
echo 4. Exit from shell 
echo 5. Verify service as follows:
echo ..   curl -XGET http://localhost:7071/healthcheck    -- Check for no ERROR --
echo ..   lynx https://multibit.org/brit/public-key       -- Check for PGP key --
echo ..
echo TIP: You can find this process again by typing 'ps -AF | grep brit'
echo TIP: If 'NoClassDefFoundError: ... BouncyCastleProvider' copy bcprov-jdk16-1.46.jar to project root
java -cp "bcprov-jdk16-1.46.jar;target/brit-service-0.0.9.jar" org.multibit.hd.brit_server.BritService server config.yml
