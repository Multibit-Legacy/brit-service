package org.multibit.site;

import com.google.common.base.Preconditions;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewMessageBodyWriter;
import org.eclipse.jetty.server.session.SessionHandler;
import org.multibit.hd.brit.matcher.*;
import org.multibit.site.health.SiteHealthCheck;
import org.multibit.site.resources.PublicBritResource;
import org.multibit.site.servlets.SafeLocaleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.File;
import java.io.IOException;

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
   * Main entry point to the application
   *
   * @param args CLI arguments
   *
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    // Securely read the password from the console
    final char[] password = readPassword();

    Matcher matcher = newMatcher(password);

    Preconditions.checkNotNull(matcher, "'matcher' must be present");

    // Must be OK to be here
    new BritService(matcher).run(args);

  }

  /**
   * <p>Initialise the Matcher</p>
   *
   * @param password The password for the Matcher secret keyring
   *
   * @throws IOException If the Matcher fails to start
   */
  private static Matcher newMatcher(char[] password) throws IOException {

    final File britMatcherDirectory = new File(BRIT_MATCHER_DIRECTORY);
    if (!britMatcherDirectory.exists()) {
      System.err.printf("Matcher directory not present at '%s'.%n", britMatcherDirectory.getAbsolutePath());
      System.exit(-1);
    }

    final File matcherStoreDirectory = new File(britMatcherDirectory, "store");
    if (!matcherStoreDirectory.exists()) {
      System.err.printf("Store directory not present at '%s'.%n", matcherStoreDirectory.getAbsolutePath());
      System.exit(-1);
    }

    File matcherSecretKeyFile = new File(britMatcherDirectory, "gpg/secring.gpg");
    if (!matcherSecretKeyFile.exists()) {
      System.err.printf("Matcher secret keyring not present at '%s'.%n", matcherSecretKeyFile.getAbsolutePath());
      System.exit(-1);
    }

    // Build the Matcher configuration
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, password);

    // Reference the Matcher store
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    // Build the Matcher
    return Matchers.newBasicMatcher(matcherConfig, matcherStore);

  }

  private static char[] readPassword() {

    Console console = System.console();
    final char[] password;
    if (console == null) {
      System.err.println("Could not obtain a console. Assuming an IDE and test data.");
      password = "password".toCharArray();
    } else {
      password = console.readPassword("[%s]", "Password:");
      if (password == null) {
        System.err.println("Could not read the password.");
        System.exit(-1);
      }
    }

    return password;

  }

  public BritService(Matcher matcher) {
    this.matcher = matcher;
  }

  @Override
  public void initialize(Bootstrap<BritConfiguration> bootstrap) {

    // Do nothing

  }

  @Override
  public void run(BritConfiguration britConfiguration, Environment environment) throws Exception {

    log.info("Scanning environment...");

    // Configure environment
    environment.addResource(new PublicBritResource(matcher));

    // Health checks
    environment.addHealthCheck(new SiteHealthCheck());

    // Providers
    environment.addProvider(new ViewMessageBodyWriter());

    // Filters
    environment.addFilter(new SafeLocaleFilter(), "/*");

    // Session handler
    environment.setSessionHandler(new SessionHandler());

  }

}
