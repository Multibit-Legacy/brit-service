package org.multibit.hd.brit.matcher;

/**
 * Copyright 2014 multibit.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.bitcoinj.core.Address;
import org.bitcoinj.params.MainNetParams;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.multibit.hd.brit.core.crypto.AESUtils;
import org.multibit.hd.brit.core.crypto.PGPUtils;
import org.multibit.hd.brit.core.dto.*;
import org.multibit.hd.brit.core.exceptions.MatcherResponseException;
import org.multibit.hd.brit.core.matcher.*;
import org.multibit.hd.brit.core.payer.BasicPayer;
import org.multibit.hd.brit.core.payer.Payer;
import org.multibit.hd.brit.core.payer.PayerConfig;
import org.multibit.hd.brit.core.payer.Payers;
import org.multibit.hd.brit.core.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.core.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.brit.crypto.PGPUtilsTest;
import org.multibit.hd.brit.dto.BRITWalletIdTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.openpgp.PGPPublicKey;

import java.io.File;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;


public class BasicMatcherTest {

  private static final Logger log = LoggerFactory.getLogger(BasicMatcherTest.class);

  private SecureRandom secureRandom;

  private static List<Address> testAddresses = Lists.newArrayList();

  @BeforeClass
  public static void setUpOnce() throws Exception {

    String[] rawTestAddresses = new String[]{

      // Good addresses
      "1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty",
      "14Ru32Lb4kdLGfAMz1VAtxh3UFku62HaNH",
      "1KesQEF2yC2FzkJYLLozZJdbBF7zRhrdSC",
      "1CuWW5fDxuFN6CcrRi51ADWHXAMJPYxY5y",
      "1NfNX36S8aocBomvWgySaK9fn93pbpEhmY",
      "1J1nTRJJT3ghsnAEvwd8dMmoTuaAMSLf4V"
    };

    for (String rawTestAddress : rawTestAddresses) {
      testAddresses.add(new Address(MainNetParams.get(), rawTestAddress));

    }

  }

  @Before
  public void setUp() throws Exception {
    secureRandom = new SecureRandom();
  }

  /**
   * Verifies the version 1 legacy interaction is still successful
   * @throws Exception If something goes wrong
   */
  @Test
  public void testPayerRequestAndMatcherResponse_Version_1_All_Good() throws Exception {

    // Create a V1 payer
    BasicPayer payer = (BasicPayer) createTestPayer();

    // Create a BRITWalletId (in real life this would be using the Payer's wallet seed)
    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed = seedGenerator.convertToSeed(Bip39SeedPhraseGenerator.split(BRITWalletIdTest.SEED_PHRASE_1));
    BRITWalletId britWalletId = new BRITWalletId(seed);

    // Create a random session id
    byte[] sessionId = new byte[AESUtils.BLOCK_LENGTH];
    secureRandom.nextBytes(sessionId);

    // Create a first transaction date (In real life this would come from a wallet)
    Optional<Date> firstTransactionDateOptional = Optional.of(new Date());

    // Create the legacy payer request (Version 1)
    PayerRequest payerRequest = payer.newLegacyPayerRequest(1, britWalletId, sessionId, firstTransactionDateOptional);
    assertThat(payerRequest).isNotNull();

    // Encrypt the PayerRequest with the Matcher PGP public key.
    EncryptedPayerRequest encryptedPayerRequest = payer.encryptPayerRequest(payerRequest);

    String payloadAsString = new String(encryptedPayerRequest.getPayload(), Charsets.UTF_8);
    log.debug("payloadAsString = \n" + payloadAsString);

    // In real life the encryptedPayerRequest is transported from the Payer to the Matcher here

    // Create a matcher that returns good addresses
    Matcher matcher = createTestMatcher_All_Good();

    // The Matcher can decrypt the EncryptedPaymentRequest using its PGP secret key
    PayerRequest matcherPayerRequest = matcher.decryptPayerRequest(encryptedPayerRequest);

    // The decrypted payment request should be the same as the original
    assertThat(payerRequest).isEqualTo(matcherPayerRequest);

    // Get the matcher to process the EncryptedPayerRequest
    MatcherResponse matcherResponse = matcher.process(matcherPayerRequest);
    assertThat(matcherResponse).isNotNull();

    // Encrypt the MatcherResponse with the AES session key
    EncryptedMatcherResponse encryptedMatcherResponse = matcher.encryptMatcherResponse(matcherResponse, payerRequest);
    assertThat(encryptedMatcherResponse).isNotNull();

    // In real life the encryptedMatcherResponse is transported from the Matcher to the Payer here

    // The payer can decrypt the encryptedMatcherResponse
    // as it knows the BRITWalletId and session id
    MatcherResponse payersMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse, payerRequest);
    assertThat(payersMatcherResponse).isNotNull();

    // The original matcher response should be the same as the decrypted version
    assertThat(matcherResponse).isEqualTo(payersMatcherResponse);
    assertThat(matcherResponse.getVersion()).isEqualTo(payerRequest.getVersion());

    // The Payer's Matcher response contains the list of addresses the Payer will use
    Set<Address> addressList = payersMatcherResponse.getBitcoinAddresses();
    assertThat(addressList).isNotNull();
    assertThat(addressList.contains(testAddresses.get(0))).isTrue();
    assertThat(addressList.contains(testAddresses.get(1))).isTrue();
    assertThat(addressList.contains(testAddresses.get(2))).isTrue();
    assertThat(addressList.contains(testAddresses.get(3))).isTrue();
    assertThat(addressList.contains(testAddresses.get(4))).isTrue();

    // The Payer's Matcher response contains a stored replay date for the wallet
    Date replayDate = payersMatcherResponse.getReplayDate().get();
    assertThat(replayDate).isNotNull();
  }

  /**
   * Verifies the version 2 interaction when all addresses are good
   *
   * @throws Exception If something goes wrong
   */
  @Test
  public void testPayerRequestAndMatcherResponse_Version_2_All_Good() throws Exception {

    // Create a standard Payer
    Payer payer = createTestPayer();

    // Create a BRITWalletId (in real life this would be using the Payer's wallet seed)
    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed = seedGenerator.convertToSeed(Bip39SeedPhraseGenerator.split(BRITWalletIdTest.SEED_PHRASE_1));
    BRITWalletId britWalletId = new BRITWalletId(seed);

    // Create a random session id
    byte[] sessionId = new byte[AESUtils.BLOCK_LENGTH];
    secureRandom.nextBytes(sessionId);

    // Create a first transaction date (In real life this would come from a wallet)
    Optional<Date> firstTransactionDateOptional = Optional.of(new Date());

    // Create the current EncryptedPayerRequest
    PayerRequest payerRequest = payer.newPayerRequest(britWalletId, sessionId, firstTransactionDateOptional);
    assertThat(payerRequest).isNotNull();

    // Encrypt the PayerRequest with the Matcher PGP public key.
    EncryptedPayerRequest encryptedPayerRequest = payer.encryptPayerRequest(payerRequest);

    String payloadAsString = new String(encryptedPayerRequest.getPayload(), Charsets.UTF_8);
    log.debug("payloadAsString = \n" + payloadAsString);

    // In real life the encryptedPayerRequest is transported from the Payer to the Matcher here

    // Create a matcher that returns all good addresses
    Matcher matcher = createTestMatcher_All_Good();

    // The Matcher can decrypt the EncryptedPaymentRequest using its PGP secret key
    PayerRequest matcherPayerRequest = matcher.decryptPayerRequest(encryptedPayerRequest);

    // The decrypted payment request should be the same as the original
    assertThat(payerRequest).isEqualTo(matcherPayerRequest);

    // Get the matcher to process the EncryptedPayerRequest
    MatcherResponse matcherResponse = matcher.process(matcherPayerRequest);
    assertThat(matcherResponse).isNotNull();

    // Encrypt the MatcherResponse with the AES session key
    EncryptedMatcherResponse encryptedMatcherResponse = matcher.encryptMatcherResponse(matcherResponse, payerRequest);
    assertThat(encryptedMatcherResponse).isNotNull();

    // In real life the encryptedMatcherResponse is transported from the Matcher to the Payer here

    // The payer can decrypt the encryptedMatcherResponse
    // as it knows the BRITWalletId and session id
    MatcherResponse payersMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse, payerRequest);
    assertThat(payersMatcherResponse).isNotNull();

    // The original matcher response should be the same as the decrypted version
    assertThat(matcherResponse).isEqualTo(payersMatcherResponse);
    assertThat(matcherResponse.getVersion()).isEqualTo(payerRequest.getVersion());

    // The Payer's Matcher response contains the list of addresses the Payer will use
    Set<Address> addressList = payersMatcherResponse.getBitcoinAddresses();
    assertThat(addressList).isNotNull();
    assertThat(addressList.contains(testAddresses.get(0))).isTrue();
    assertThat(addressList.contains(testAddresses.get(1))).isTrue();
    assertThat(addressList.contains(testAddresses.get(2))).isTrue();
    assertThat(addressList.contains(testAddresses.get(3))).isTrue();
    assertThat(addressList.contains(testAddresses.get(4))).isTrue();
    assertThat(addressList.contains(testAddresses.get(5))).isTrue();

    // The Payer's Matcher response contains a stored replay date for the wallet
    Date replayDate = payersMatcherResponse.getReplayDate().get();
    assertThat(replayDate).isNotNull();
  }

  /**
   * Verifies the Version 2 interaction when one address is bad (server damaged)
   * @throws Exception
   */
  @Test(expected = MatcherResponseException.class)
  public void testPayerRequestAndMatcherResponse_Version_2_One_Bad() throws Exception {

    // Create a standard Payer
    Payer payer = createTestPayer();

    // Create a BRITWalletId (in real life this would be using the Payer's wallet seed)
    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed = seedGenerator.convertToSeed(Bip39SeedPhraseGenerator.split(BRITWalletIdTest.SEED_PHRASE_1));
    BRITWalletId britWalletId = new BRITWalletId(seed);

    // Create a random session id
    byte[] sessionId = new byte[AESUtils.BLOCK_LENGTH];
    secureRandom.nextBytes(sessionId);

    // Create a first transaction date (In real life this would come from a wallet)
    Optional<Date> firstTransactionDateOptional = Optional.of(new Date());

    // Create the current EncryptedPayerRequest
    PayerRequest payerRequest = payer.newPayerRequest(britWalletId, sessionId, firstTransactionDateOptional);
    assertThat(payerRequest).isNotNull();

    // Encrypt the PayerRequest with the Matcher PGP public key.
    EncryptedPayerRequest encryptedPayerRequest = payer.encryptPayerRequest(payerRequest);

    String payloadAsString = new String(encryptedPayerRequest.getPayload(), Charsets.UTF_8);
    log.debug("payloadAsString = \n" + payloadAsString);

    // In real life the encryptedPayerRequest is transported from the Payer to the Matcher here

    // Create a matcher that returns one bad address
    Matcher matcher = createTestMatcher_One_Bad();

    // The Matcher can decrypt the EncryptedPaymentRequest using its PGP secret key
    PayerRequest matcherPayerRequest = matcher.decryptPayerRequest(encryptedPayerRequest);

    // The decrypted payment request should be the same as the original
    assertThat(payerRequest).isEqualTo(matcherPayerRequest);

    // Get the matcher to process the EncryptedPayerRequest
    MatcherResponse matcherResponse = matcher.process(matcherPayerRequest);
    assertThat(matcherResponse).isNotNull();

    // Introduce a mangled MatcherResponse (simulates a hardware fault on the server)
    MatcherResponse mangledMatcherResponse = new MatcherResponse(
      matcherPayerRequest.getVersion(),
      matcherResponse.getReplayDate(),
      matcherResponse.getBitcoinAddresses()
    ) {

      @Override
      public byte[] serialise() {
        StringBuilder builder = new StringBuilder();
        builder.append(getVersion()).append(PayerRequest.SEPARATOR);
        if (getReplayDate().isPresent()) {
          builder.append(getReplayDate().get().getTime()).append(PayerRequest.SEPARATOR);
        } else {
          builder.append(OPTIONAL_NOT_PRESENT_TEXT).append(PayerRequest.SEPARATOR);
        }
        if (getBitcoinAddresses() != null) {
          for (Address address : getBitcoinAddresses()) {
            builder.append(address).append(PayerRequest.SEPARATOR);
          }
        }

        // Append a bad address
        builder.append("1XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX").append(PayerRequest.SEPARATOR);

        return builder.toString().getBytes(Charsets.UTF_8);

      }
    };

    // Encrypt the mangled MatcherResponse with the AES session key
    EncryptedMatcherResponse encryptedMatcherResponse = matcher.encryptMatcherResponse(mangledMatcherResponse, payerRequest);
    assertThat(encryptedMatcherResponse).isNotNull();

    // In real life the encryptedMatcherResponse is transported from the Matcher to the Payer here

    // The payer can decrypt the encryptedMatcherResponse
    // as it knows the BRITWalletId and session id
    // We expect an exception due to the malformed address within
    payer.decryptMatcherResponse(encryptedMatcherResponse, payerRequest);

    fail("Expected exception");

  }

  private Matcher createTestMatcher_All_Good() throws Exception {

    // Find the example Matcher PGP secret key ring file
    File matcherSecretKeyFile = PGPUtilsTest.makeFile(PGPUtilsTest.TEST_MATCHER_SECRET_KEYRING_FILE);
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, PGPUtilsTest.TEST_DATA_PASSWORD);

    // Create a random temporary directory for the Matcher store to use
    File matcherStoreDirectory = Files.createTempDir();
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    Matcher matcher = Matchers.newBasicMatcher(matcherConfig, matcherStore);
    assertThat(matcher).isNotNull();

    // Add some test data for today's bitcoin addresses
    Set<Address> bitcoinAddresses = Sets.newHashSet();
    // Good addresses
    bitcoinAddresses.add(testAddresses.get(0));
    bitcoinAddresses.add(testAddresses.get(1));
    bitcoinAddresses.add(testAddresses.get(2));
    bitcoinAddresses.add(testAddresses.get(3));
    bitcoinAddresses.add(testAddresses.get(4));
    bitcoinAddresses.add(testAddresses.get(5));

    matcherStore.storeBitcoinAddressesForDate(bitcoinAddresses, new Date());

    return matcher;
  }

  private Matcher createTestMatcher_One_Bad() throws Exception {

    // Find the example Matcher PGP secret key ring file
    File matcherSecretKeyFile = PGPUtilsTest.makeFile(PGPUtilsTest.TEST_MATCHER_SECRET_KEYRING_FILE);
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, PGPUtilsTest.TEST_DATA_PASSWORD);

    // Create a random temporary directory for the Matcher store to use
    File matcherStoreDirectory = Files.createTempDir();
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    Matcher matcher = Matchers.newBasicMatcher(matcherConfig, matcherStore);
    assertThat(matcher).isNotNull();

    // Add some test data for today's bitcoin addresses
    Set<Address> bitcoinAddresses = Sets.newHashSet();
    // Good addresses
    bitcoinAddresses.add(testAddresses.get(0));
    bitcoinAddresses.add(testAddresses.get(1));
    bitcoinAddresses.add(testAddresses.get(2));
    bitcoinAddresses.add(testAddresses.get(3));
    bitcoinAddresses.add(testAddresses.get(4));

    matcherStore.storeBitcoinAddressesForDate(bitcoinAddresses, new Date());

    return matcher;
  }

  private Payer createTestPayer() throws Exception {

    // Load the example Matcher PGP public key
    File matcherPublicKeyFile = PGPUtilsTest.makeFile(PGPUtilsTest.TEST_MATCHER_PUBLIC_KEY_FILE);
    FileInputStream matcherPublicKeyInputStream = new FileInputStream(matcherPublicKeyFile);
    PGPPublicKey matcherPGPPublicKey = PGPUtils.readPublicKey(matcherPublicKeyInputStream);

    PayerConfig payerConfig = new PayerConfig(matcherPGPPublicKey);

    Payer payer = Payers.newBasicPayer(payerConfig);
    assertThat(payer).isNotNull();

    return payer;
  }

}
