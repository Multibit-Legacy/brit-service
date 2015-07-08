package org.multibit.hd.brit.rest.health;

/**
 * <p>HealthCheck to provide the following to application:</p>
 * <ul>
 * <li>Verifies that BRIT service can round trip a request correctly</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */

import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.yammer.metrics.core.HealthCheck;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.multibit.hd.brit.core.crypto.AESUtils;
import org.multibit.hd.brit.core.crypto.PGPUtils;
import org.multibit.hd.brit.core.dto.*;
import org.multibit.hd.brit.core.payer.Payer;
import org.multibit.hd.brit.core.payer.PayerConfig;
import org.multibit.hd.brit.core.payer.Payers;
import org.multibit.hd.brit.core.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.core.seed_phrase.SeedPhraseGenerator;
import org.spongycastle.openpgp.PGPPublicKey;

import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

public class BritMatcherHealthCheck extends HealthCheck {

  private SecureRandom secureRandom = new SecureRandom();

  public BritMatcherHealthCheck() {
    super("BRIT matcher health check");
  }

  @Override
  protected Result check() throws Exception {

    MatcherResponse matcherResponse = createAndRegisterWalletId();

    int size = matcherResponse.getBitcoinAddresses().size();

    if (size < 50) {
      return Result.unhealthy("Matcher response contained only "+size+" addresses. Expected 50 or more.");
    }

    // Must be OK to be here
    return Result.healthy("Matcher response contains %d addresses", size);
  }

  /**
   * Create a new random WalletId and
   *
   * @return The Matcher response from the BRIT server
   *
   */
  private MatcherResponse createAndRegisterWalletId() throws Exception {

    // Create a payer and a wallet Id
    Payer payer = newTestPayer();
    BRITWalletId britWalletId = newBritWalletId();

    // Create a random session Id
    byte[] sessionId = newSessionId();

    // Create a first transaction date (in real life this would come from a wallet)
    Optional<Date> firstTransactionDateOptional = Optional.of(new Date());

    // Ask the payer to create an EncryptedPayerRequest containing a BRITWalletId, a session id and a firstTransactionDate
    PayerRequest payerRequest = payer.newPayerRequest(
      britWalletId,
      sessionId,
      firstTransactionDateOptional
    );
    if (payerRequest == null) {
      throw new Exception("Could not create PayerRequest");
    }

    // Encrypt the PayerRequest with the Matcher PGP public key
    EncryptedPayerRequest encryptedPayerRequest = payer.encryptPayerRequest(payerRequest);

    byte[] payload = encryptedPayerRequest.getPayload();

    // Send the encrypted request to the Matcher
    Client client = new Client();
    byte[] actualResponse = client
      .resource("http://localhost:7070/brit")
      .header("Content-Type", "application/octet-stream")
      .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
      .entity(payload)
      .post(byte[].class);
    if (actualResponse.length <= 20) {
      throw new Exception("POST response is 204 NO CONTENT");
    }

    // Build the encrypted Matcher response
    EncryptedMatcherResponse encryptedMatcherResponse = new EncryptedMatcherResponse(actualResponse);

    // Payer can decrypt the encryptedMatcherResponse because it knows the BRITWalletId and session id
    MatcherResponse plainMatcherResponse = payer.decryptMatcherResponse(encryptedMatcherResponse, payerRequest);
    if (plainMatcherResponse == null) {
      throw new Exception("Could not decrypt matcher response (null)");
    }
    if (plainMatcherResponse.getBitcoinAddresses() == null) {
      throw new Exception("Matcher response does not contain Bitcoin addresses (null)");
    }
    if (plainMatcherResponse.getBitcoinAddresses().isEmpty()) {
      throw new Exception("Matcher response does not contain Bitcoin addresses (empty)");
    }

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
  @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
  private Payer newTestPayer() throws Exception {

    // Load the live matcher public key
    InputStream matcherPublicKeyInputStream = new FileInputStream("/var/brit/matcher/gpg/matcher-key.asc");
    PGPPublicKey matcherPGPPublicKey = PGPUtils.readPublicKey(matcherPublicKeyInputStream);

    PayerConfig payerConfig = new PayerConfig(matcherPGPPublicKey);

    // Create the Payer
    return Payers.newBasicPayer(payerConfig);

  }

}