package org.multibit.hd.brit_server;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.LoggingFactory;
import com.yammer.dropwizard.views.ViewMessageBodyWriter;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.jetty.server.session.SessionHandler;
import org.multibit.hd.brit.crypto.PGPUtils;
import org.multibit.hd.brit.matcher.*;
import org.multibit.hd.brit_server.health.SiteHealthCheck;
import org.multibit.hd.brit_server.resources.PublicBritResource;
import org.multibit.hd.brit_server.resources.RuntimeExceptionMapper;
import org.multibit.hd.brit_server.servlets.AddressThrottlingFilter;
import org.multibit.hd.brit_server.servlets.SafeLocaleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * <p>Service to provide the following to application:</p>
 * <ul>
 * <li>Provision of access to resources</li>
 * </ul>
 * <p>Use <code>java -jar site-develop-SNAPSHOT.jar server site-config.yml</code> to start the demo</p>
 *
 * @since 0.0.1
 *  
 */
public class BritService extends Service<BritConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(BritService.class);

  /**
   * The BRIT Matcher root directory
   */
  private static final String BRIT_MATCHER_DIRECTORY = "/var/brit/matcher";

  /**
   * The Matcher
   */
  private final Matcher matcher;

  /**
   * The Matcher public key
   */
  private final String matcherPublicKey;

  /**
   * Main entry point to the application
   *
   * @param args CLI arguments
   *
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    // Start the logging factory
    LoggingFactory.bootstrap();

    // Securely read the password from the console
    final char[] password = readPassword();

    System.out.print("Crypto files ");
    // PGP decrypt the file (requires the private key ring that is password protected)
    final File britMatcherDirectory = getBritMatcherDirectory();
    final File matcherSecretKeyringFile = getMatcherSecretKeyringFile(britMatcherDirectory);
    final File matcherPublicKeyFile = getMatcherPublicKeyFile(britMatcherDirectory);
    final File testCryptoFile = getTestCryptoFile(britMatcherDirectory);
    System.out.println("OK");

    System.out.print("Crypto keys ");
    try {
      // Attempt to encrypt the test file
      ByteArrayOutputStream armoredOut = new ByteArrayOutputStream(1024);
      PGPPublicKey matcherPublicKey = PGPUtils.readPublicKey(new FileInputStream(matcherPublicKeyFile));
      PGPUtils.encryptFile(armoredOut, testCryptoFile, matcherPublicKey);

      // Attempt to decrypt the test file
      ByteArrayInputStream armoredIn = new ByteArrayInputStream(armoredOut.toByteArray());
      ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream(1024);
      PGPUtils.decryptFile(armoredIn, decryptedOut, new FileInputStream(matcherSecretKeyringFile), password);

      // Verify that the decryption was successful
      String testCrypto = decryptedOut.toString();
      System.out.println(testCrypto);

      if (!"OK".equals(testCrypto)) {
        System.err.println("FAIL");
        System.exit(-1);
      }

    } catch (PGPException e) {
      System.err.println("FAIL (" + e.getMessage() + "). Checksum means password is incorrect.");
      System.exit(-1);
    }

    // Create the Matcher
    System.out.print("Matcher ");
    Matcher matcher = newMatcher(password);
    Preconditions.checkNotNull(matcher, "'matcher' must be present");
    System.out.println("OK\nStarting server...\n");

    // Load the public key
    String matcherPublicKey = Files.toString(matcherPublicKeyFile, Charsets.UTF_8);

    // Must be OK to be here
    new BritService(matcher, matcherPublicKey).run(args);

  }

  private static char[] readPassword() {

    Console console = System.console();
    final char[] password;
    if (console == null) {
      System.out.println("Could not obtain a console. Assuming an IDE and test data.");
      password = "password".toCharArray();
    } else {
      password = console.readPassword("%s", "Enter password:");
      if (password == null) {
        System.err.println("Could not read the password.");
        System.exit(-1);
      }
      System.out.println("Working...");
    }

    return password;

  }

  /**
   * <p>Initialise the Matcher</p>
   *
   * @param password The password for the Matcher secret keyring
   *
   * @throws IOException If the Matcher fails to start
   */
  private static Matcher newMatcher(char[] password) throws IOException {

    final File britMatcherDirectory = getBritMatcherDirectory();
    final File matcherStoreDirectory = getMatcherStoreDirectory(britMatcherDirectory);
    final File matcherSecretKeyFile = getMatcherSecretKeyringFile(britMatcherDirectory);

    // Build the Matcher configuration
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, password);

    // Reference the Matcher store
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    // Build the Matcher
    return Matchers.newBasicMatcher(matcherConfig, matcherStore);

  }

  private static File getBritMatcherDirectory() {

    final File britMatcherDirectory = new File(BRIT_MATCHER_DIRECTORY);
    if (!britMatcherDirectory.exists()) {
      System.err.printf("Matcher directory not present at '%s'.%n", britMatcherDirectory.getAbsolutePath());
      System.exit(-1);
    }

    return britMatcherDirectory;
  }

  private static File getMatcherStoreDirectory(File britMatcherDirectory) {

    final File matcherStoreDirectory = new File(britMatcherDirectory, "store");
    if (!matcherStoreDirectory.exists()) {
      System.err.printf("Store directory not present at '%s'.%n", matcherStoreDirectory.getAbsolutePath());
      System.exit(-1);
    }

    return matcherStoreDirectory;
  }

  private static File getMatcherSecretKeyringFile(File britMatcherDirectory) {

    File matcherSecretKeyringFile = new File(britMatcherDirectory, "gpg/secring.gpg");
    if (!matcherSecretKeyringFile.exists()) {
      System.err.printf("Matcher secret keyring not present at '%s'.%n", matcherSecretKeyringFile.getAbsolutePath());
      System.exit(-1);
    }

    return matcherSecretKeyringFile;
  }

  private static File getMatcherPublicKeyFile(File britMatcherDirectory) {

    File matcherPublicKeyFile = new File(britMatcherDirectory, "gpg/matcher-key.asc");
    if (!matcherPublicKeyFile.exists()) {
      System.err.printf("Matcher public key not present at '%s'.%n", matcherPublicKeyFile.getAbsolutePath());
      System.exit(-1);
    }

    return matcherPublicKeyFile;
  }

  private static File getTestCryptoFile(File britMatcherDirectory) throws IOException {

    File testCryptoFile = new File(britMatcherDirectory, "gpg/test.txt");
    if (!testCryptoFile.exists()) {
      if (!testCryptoFile.createNewFile()) {
        System.err.printf("Could not create crypto test file: '%s'.%n", testCryptoFile.getAbsolutePath());
        System.exit(-1);
      }
      // Populate it with a simple test
      Writer writer = new FileWriter(testCryptoFile);
      writer.write("OK");
      writer.flush();
      writer.close();

    }

    return testCryptoFile;
  }

  public BritService(Matcher matcher, String matcherPublicKey) {
    this.matcher = matcher;
    this.matcherPublicKey = matcherPublicKey;
  }

  @Override
  public void initialize(Bootstrap<BritConfiguration> bootstrap) {

    // Do nothing

  }

  @Override
  public void run(BritConfiguration britConfiguration, Environment environment) throws Exception {

    log.info("Scanning environment...");

    // Configure environment
    environment.addResource(new PublicBritResource(matcher, matcherPublicKey));

    // Health checks
    environment.addHealthCheck(new SiteHealthCheck());

    // Providers
    environment.addProvider(new ViewMessageBodyWriter());
    environment.addProvider(new RuntimeExceptionMapper());

    // Filters
    environment.addFilter(new SafeLocaleFilter(), "/*");
    environment.addFilter(new AddressThrottlingFilter(), "/*");

    // Session handler
    environment.setSessionHandler(new SessionHandler());

  }

}
