package org.multibit.site.resources;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.yammer.dropwizard.testing.ResourceTest;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.Test;
import org.multibit.hd.brit.crypto.AESUtils;
import org.multibit.hd.brit.crypto.PGPUtils;
import org.multibit.hd.brit.dto.*;
import org.multibit.hd.brit.matcher.*;
import org.multibit.hd.brit.payer.Payer;
import org.multibit.hd.brit.payer.PayerConfig;
import org.multibit.hd.brit.payer.Payers;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.brit.utils.FileUtils;
import org.multibit.site.testing.FixtureAsserts;
import org.multibit.site.testing.FixtureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class PublicBritResourceTest extends ResourceTest {

  private static final Logger log = LoggerFactory.getLogger(PublicBritResourceTest.class);

  public static final String SEED_PHRASE_1 = "letter advice cage absurd amount doctor acoustic avoid letter advice cage above";
  private static final String WALLET_ID_1 = "4bbd8a749179d65a5f1b0859684f53ba5b761714";

  public static final String SEED_PHRASE_2 = "require want tube elegant juice cool cup noble town poem plate harsh";
  private static final String WALLET_ID_2 = "7e5218ea0428cbd44de74567fd8af557d8715545";

  private static final String SEED_PHRASE_3 = "morning truly witness grass pill typical blur then notable session exact coyote word noodle dentist hurry ability dignity";
  private static final String WALLET_ID_3 = "b1de12bdf20f332144851da717ae32c8aebcadb7";

  private static final String EXAMPLE_TEXT = "The quick brown fox jumps over the lazy dog. 01234567890. !@#$%^&*(). ,.;:[]-_=+";

  public static final String TEST_MATCHER_PUBLIC_KEYRING_FILE = "/src/test/resources/matcher/gpg/pubring.gpg";

  public static final String TEST_MATCHER_SECRET_KEYRING_FILE = "/src/test/resources/matcher/gpg/secring.gpg";

  public static final String TEST_MATCHER_PUBLIC_KEY_FILE = "/src/test/resources/matcher/export-to-payer/matcher-key.asc";

  /**
   * The password used in the generation of the test PGP keys
   */
  public static final char[] TEST_DATA_PASSWORD = "password".toCharArray();

  private PublicBritResource testObject;

  private SecureRandom secureRandom;

  @Override
  protected void setUpResources() throws Exception {

    Matcher matcher = createTestMatcher();

    testObject = new PublicBritResource(matcher);

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
      "/brit/matcher-pubkey.asc"
    );

  }

  @Test
  public void POST_EncryptedPayerRequest() throws Exception {

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
    MatcherResponse plainMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse);
    assertThat(plainMatcherResponse).isNotNull();

    // Get the list of addresses the Payer will use
    List<String> addressList = plainMatcherResponse.getAddressList();
    assertThat(addressList).isNotNull();

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
    InputStream matcherPublicKeyInputStream = PublicBritResource.class.getResourceAsStream("/brit/matcher-pubkey.asc");
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
    File matcherSecretKeyFile = FixtureUtils.makeFile("",TEST_MATCHER_SECRET_KEYRING_FILE);
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, TEST_DATA_PASSWORD);

    // Create a random temporary directory for the Matcher store to use
    File matcherStoreDirectory = FileUtils.makeRandomTemporaryDirectory();
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    Matcher matcher = Matchers.newBasicMatcher(matcherConfig, matcherStore);
    assertThat(matcher).isNotNull();

    // Add some test data for today's bitcoin addresses
    List<String> bitcoinAddressList = Lists.newArrayList();
    bitcoinAddressList.add("cat");
    bitcoinAddressList.add("dog");
    bitcoinAddressList.add("elephant");
    bitcoinAddressList.add("worm");

    matcherStore.storeBitcoinAddressListForDate(bitcoinAddressList, new Date());

    return matcher;
  }

}
