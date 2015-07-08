package org.multibit.hd.brit.rest.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.yammer.dropwizard.testing.ResourceTest;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Test;
import org.multibit.hd.brit.core.crypto.AESUtils;
import org.multibit.hd.brit.core.crypto.PGPUtils;
import org.multibit.hd.brit.core.dto.*;
import org.multibit.hd.brit.core.matcher.*;
import org.multibit.hd.brit.core.payer.Payer;
import org.multibit.hd.brit.core.payer.PayerConfig;
import org.multibit.hd.brit.core.payer.Payers;
import org.multibit.hd.brit.core.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.core.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.brit.rest.utils.StreamUtils;
import org.multibit.hd.brit_rest.testing.FixtureAsserts;
import org.multibit.hd.brit_rest.testing.FixtureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.openpgp.PGPPublicKey;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;

public class PublicBritResourceTest extends ResourceTest {

  private static final Logger log = LoggerFactory.getLogger(PublicBritResourceTest.class);

  private static SecureRandom secureRandom = new SecureRandom();

  public static final String SEED_PHRASE_1 = "letter advice cage absurd amount doctor acoustic avoid letter advice cage above";

  public static final String TEST_MATCHER_SECRET_KEYRING_FILE = "/src/test/resources/matcher/gpg/secring.gpg";

  /**
   * The password used in the generation of the test PGP keys
   */
  public static final char[] TEST_DATA_PASSWORD = "password".toCharArray();

  @Override
  protected void setUpResources() throws Exception {

    Matcher matcher = createTestMatcher();

    String matcherPublicKey = StreamUtils.toString(PublicBritResource.class.getResourceAsStream("/matcher/gpg/matcher-key.asc"));

    PublicBritResource testObject = new PublicBritResource(matcher, matcherPublicKey);

    // Configure resources
    addResource(testObject);

    secureRandom = new SecureRandom();

  }

  @Test
  public void GET_MatcherPublicKey() throws Exception {

    // Build the request
    String actualResponse = client()
      .resource("/brit/public-key")
      .header("Content-Type", "text/plain")
      .accept(MediaType.TEXT_PLAIN_TYPE)
      .get(String.class);

    FixtureAsserts.assertStringMatchesStringFixture(
      "Get Matcher public key",
      actualResponse,
      "/matcher/gpg/matcher-key.asc"
    );

  }

  @Test
  public void POST_EncryptedPayerRequest_Binary() throws Exception {

    // Create a payer
    Payer payer = newTestPayer();

    BRITWalletId britWalletId = newBritWalletId();

    // Create a random session id
    byte[] sessionId = newSessionId();

    // Create a first transaction date (in real life this would come from a wallet)
    Optional<Date> firstTransactionDateOptional = Optional.of(new Date());

    // Ask the payer to create an EncryptedPayerRequest containing a BRITWalletId, a session id and a firstTransactionDate
    PayerRequest payerRequest = payer.newPayerRequest(britWalletId, sessionId, firstTransactionDateOptional);
    assertThat(payerRequest).isNotNull();
    // Encrypt the PayerRequest with the Matcher PGP public key.
    EncryptedPayerRequest encryptedPayerRequest = payer.encryptPayerRequest(payerRequest);

    byte[] payload = encryptedPayerRequest.getPayload();

    // Send the encrypted request to the Matcher
    byte[] actualResponse = client()
      .resource("/brit")
      .header("Content-Type", "application/octet-stream")
      .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
      .entity(payload)
      .post(byte[].class);

    assertThat(actualResponse.length).isGreaterThanOrEqualTo(20);

    // Build the encrypted Matcher response
    EncryptedMatcherResponse encryptedMatcherResponse = new EncryptedMatcherResponse(actualResponse);

    // Payer can decrypt the encryptedMatcherResponse because it knows the BRITWalletId and session id
    MatcherResponse plainMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse, payerRequest);
    assertThat(plainMatcherResponse).isNotNull();

    // Get the list of addresses the Payer will use
    Set<Address> bitcoinAddresses = plainMatcherResponse.getBitcoinAddresses();
    assertThat(bitcoinAddresses).isNotNull();

    // Get the replay date for the wallet
    Date replayDate = plainMatcherResponse.getReplayDate().get();
    assertThat(replayDate).isNotNull();

  }

  @Test
  public void POST_EncryptedPayerRequest_String() throws Exception {

    // Create a payer
    Payer payer = newTestPayer();

    BRITWalletId britWalletId = newBritWalletId();

    // Create a random session id
    byte[] sessionId = newSessionId();

    // Create a first transaction date (in real life this would come from a wallet)
    Optional<Date> firstTransactionDateOptional = Optional.of(new Date());

    // Ask the payer to create an EncryptedPayerRequest containing a BRITWalletId, a session id and a firstTransactionDate
    PayerRequest payerRequest = payer.newPayerRequest(britWalletId, sessionId, firstTransactionDateOptional);
    assertThat(payerRequest).isNotNull();
    // Encrypt the PayerRequest with the Matcher PGP public key.
    EncryptedPayerRequest encryptedPayerRequest = payer.encryptPayerRequest(payerRequest);

    String payload = new String(encryptedPayerRequest.getPayload(), Charsets.UTF_8);

    // Send the encrypted request to the Matcher
    byte[] actualResponse = client()
      .resource("/brit")
      .header("Content-Type", "text/plain")
      .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
      .entity(payload)
      .post(byte[].class);

    assertThat(actualResponse.length).isGreaterThanOrEqualTo(20);

    // Build the encrypted Matcher response
    EncryptedMatcherResponse encryptedMatcherResponse = new EncryptedMatcherResponse(actualResponse);

    // Payer can decrypt the encryptedMatcherResponse because it knows the BRITWalletId and session id
    MatcherResponse plainMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse, payerRequest);
    assertThat(plainMatcherResponse).isNotNull();

    // Get the list of addresses the Payer will use
    Set<Address> bitcoinAddresses = plainMatcherResponse.getBitcoinAddresses();
    assertThat(bitcoinAddresses).isNotNull();

    // Get the replay date for the wallet
    Date replayDate = plainMatcherResponse.getReplayDate().get();
    assertThat(replayDate).isNotNull();

  }

  private byte[] newSessionId() {
    byte[] sessionId = new byte[AESUtils.BLOCK_LENGTH];
    secureRandom.nextBytes(sessionId);
    return sessionId;
  }

  private BRITWalletId newBritWalletId() {
    // Create a BRIT WalletId (in real life this would be using the Payer's wallet seed)
    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed = seedGenerator.convertToSeed(Bip39SeedPhraseGenerator.split(PublicBritResourceTest.SEED_PHRASE_1));
    return new BRITWalletId(seed);
  }


  /**
   * @return A test Payer
   *
   * @throws Exception If something goes wrong
   */
  private Payer newTestPayer() throws Exception {

    // Load the example Matcher PGP public key
    InputStream matcherPublicKeyInputStream = PublicBritResource.class.getResourceAsStream("/matcher/gpg/matcher-key.asc");
    PGPPublicKey matcherPGPPublicKey = PGPUtils.readPublicKey(matcherPublicKeyInputStream);

    log.info("Matcher public key id = " + matcherPGPPublicKey.getKeyID());

    PayerConfig payerConfig = new PayerConfig(matcherPGPPublicKey);

    // Create and verify the Payer
    Payer payer = Payers.newBasicPayer(payerConfig);
    assertThat(payer).isNotNull();
    assertThat(payer.getConfig().getMatcherPublicKey()).isEqualTo(matcherPGPPublicKey);

    return payer;
  }

  /**
   * @return A test Matcher
   *
   * @throws Exception If something goes wrong
   */
  private Matcher createTestMatcher() throws Exception {

    // Find the example Matcher PGP secret key ring file
    File matcherSecretKeyFile = FixtureUtils.makeFile("", TEST_MATCHER_SECRET_KEYRING_FILE);
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, TEST_DATA_PASSWORD);

    // Create a random temporary directory for the Matcher store to use
    File matcherStoreDirectory = PublicBritResourceTest.createTemporaryDirectory();
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    Matcher matcher = Matchers.newBasicMatcher(matcherConfig, matcherStore);
    assertThat(matcher).isNotNull();

    // Add some test data for today's bitcoin addresses
    Set<Address> bitcoinAddresses = Sets.newHashSet();
    NetworkParameters mainNet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
    bitcoinAddresses.add(new Address(mainNet, "1MkTpZN4TpLwJjZt9zHBXREJA8avUHXB3q"));
    bitcoinAddresses.add(new Address(mainNet, "1WGmwv86m1fFNVDRQ2YagdAFCButd36SV"));
    bitcoinAddresses.add(new Address(mainNet, "1PP1BvDeXjUcPDiEHBPWptQBAukhAwsLFt"));
    bitcoinAddresses.add(new Address(mainNet, "128f69V7GRqNSKwrjMkcuB6dbFKKEPtaLC"));


    matcherStore.storeBitcoinAddressesForDate(bitcoinAddresses, new Date());

    return matcher;
  }

  /**
    * <p>Atomically create a temporary directory that will be removed when the JVM exits</p>
    *
    * @return A random temporary directory
    * @throws java.io.IOException If something goes wrong
    */
   public static File createTemporaryDirectory() throws IOException {

     // Use JDK7 NIO Files for a more secure operation than Guava
     File topLevelTemporaryDirectory = Files.createTempDirectory("mbhd").toFile();

     topLevelTemporaryDirectory.deleteOnExit();

     // Add a random number to the topLevelTemporaryDirectory
     String temporaryDirectoryName = topLevelTemporaryDirectory.getAbsolutePath() + File.separator + secureRandom.nextInt(Integer.MAX_VALUE);
     log.debug("Temporary directory name:\n'{}'", temporaryDirectoryName);
     File temporaryDirectory = new File(temporaryDirectoryName);
     temporaryDirectory.deleteOnExit();

     if (temporaryDirectory.mkdir() && temporaryDirectory.exists() && temporaryDirectory.canWrite() && temporaryDirectory.canRead()) {
       log.debug("Created temporary directory:\n'{}'", temporaryDirectory.getAbsolutePath());
       return temporaryDirectory;
     }

     // Must have failed to be here
     throw new IOException("Did not create '" + temporaryDirectory.getAbsolutePath() + "' with RW permissions");
   }
}