package org.multibit.hd.brit_server.resources;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.*;
import com.sun.jersey.api.client.Client;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.multibit.hd.brit.crypto.AESUtils;
import org.multibit.hd.brit.crypto.PGPUtils;
import org.multibit.hd.brit.dto.*;
import org.multibit.hd.brit.payer.Payer;
import org.multibit.hd.brit.payer.PayerConfig;
import org.multibit.hd.brit.payer.Payers;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.fest.assertions.api.Assertions.assertThat;

public class PublicBritResourceLoadTest {

  private static final Logger log = LoggerFactory.getLogger(PublicBritResourceLoadTest.class);

  private SecureRandom secureRandom = new SecureRandom();

  /**
   * Entry point to the load tester
   *
   * @param args The command line arguments
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    PublicBritResourceLoadTest loadTest = new PublicBritResourceLoadTest();

    loadTest.start();

  }

  private void start() {

    int MAX_EXECUTORS = 200;

    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));

    for (int i = 0; i < MAX_EXECUTORS; i++) {

      ListenableFuture<MatcherResponse> matcherResponse = service.submit(new Callable<MatcherResponse>() {

        @Override
        public MatcherResponse call() throws Exception {

          return createAndRegisterWalletId();

        }
      });

      Futures.addCallback(matcherResponse, new FutureCallback<MatcherResponse>() {

        public void onSuccess(MatcherResponse matcherResponse) {
          log.info("SUCCESS. Address 0: {}", matcherResponse.getBitcoinAddresses().iterator().next());
        }

        public void onFailure(Throwable thrown) {
          log.error("FAILURE " + thrown.getMessage());
        }
      });

    }
  }

  /**
   * Create a new random WalletId and
   *
   * @return The Matcher response from the BRIT server
   *
   */
  public synchronized MatcherResponse createAndRegisterWalletId() throws Exception {

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
    log.info("Posting to server...");
    Client client = new Client();
    byte[] actualResponse = client
      .resource("http://localhost:9090/brit")
      .header("Content-Type", "application/octet-stream")
      .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
      .entity(payload)
      .post(byte[].class);
    log.info("Posted.");

    assertThat(actualResponse.length).describedAs("Check for 204_NO_CONTENT may have address throttling active.").isGreaterThanOrEqualTo(20);

    // Build the encrypted Matcher response
    EncryptedMatcherResponse encryptedMatcherResponse = new EncryptedMatcherResponse(actualResponse);

    // Payer can decrypt the encryptedMatcherResponse because it knows the BRITWalletId and session id
    MatcherResponse plainMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse);
    assertThat(plainMatcherResponse).isNotNull();

    assertThat(plainMatcherResponse.getBitcoinAddresses()).isNotNull();

    assertThat(plainMatcherResponse.getBitcoinAddresses().isEmpty()).isFalse();

    return plainMatcherResponse;

  }


  private byte[] newSessionId() {

    byte[] sessionId = new byte[AESUtils.BLOCK_LENGTH];
    secureRandom.nextBytes(sessionId);

    return sessionId;
  }

  private BRITWalletId newBritWalletId() {

    // Create a BRIT WalletId (in real life this would be using the Payer's wallet seed)
    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    List<String> seedPhrase = seedGenerator.newSeedPhrase();
    byte[] seed = seedGenerator.convertToSeed(seedPhrase);

    return new BRITWalletId(seed);
  }


  /**
   * @return A test Payer
   *
   * @throws Exception If something goes wrong
   */
  private Payer newTestPayer() throws Exception {

    // Load the example Matcher PGP public key
    InputStream matcherPublicKeyInputStream = PublicBritResource.class.getResourceAsStream("/brit/test-matcher-key.asc");
    PGPPublicKey matcherPGPPublicKey = PGPUtils.readPublicKey(matcherPublicKeyInputStream);

    log.info("Matcher public key id = " + matcherPGPPublicKey.getKeyID());

    PayerConfig payerConfig = new PayerConfig(matcherPGPPublicKey);

    // Create and verify the Payer
    Payer payer = Payers.newBasicPayer(payerConfig);
    assertThat(payer).isNotNull();
    assertThat(payer.getConfig().getMatcherPublicKey()).isEqualTo(matcherPGPPublicKey);

    return payer;
  }

}
