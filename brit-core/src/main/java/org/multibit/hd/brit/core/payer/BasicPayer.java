package org.multibit.hd.brit.core.payer;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.KeyCrypterException;
import org.multibit.commons.crypto.AESUtils;
import org.multibit.commons.crypto.PGPUtils;
import org.multibit.hd.brit.core.dto.*;
import org.multibit.hd.brit.core.exceptions.MatcherResponseException;
import org.multibit.hd.brit.core.exceptions.PayerRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.openpgp.PGPException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Date;

/**
 * <p>Payer to provide the following to BRIT:</p>
 * <ul>
 * <li>Implementation of a basic Payer</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class BasicPayer implements Payer {

  private static final Logger log = LoggerFactory.getLogger(BasicPayer.class);

  /**
   * This is the default version and should be the highest supported value
   */
  public static final int CURRENT_BRIT_VERSION = 2;

  private PayerConfig payerConfig;
  private BRITWalletId britWalletId;
  private byte[] sessionKey;

  public BasicPayer(PayerConfig payerConfig) {
    this.payerConfig = payerConfig;
  }

  public PayerConfig getConfig() {
    return payerConfig;
  }

  /**
   * Create a legacy version of a Payer to allow backwards compatibility
   *
   * This is not present on the Payer interface to avoid accidental use
   *
   * @param version              The version to use
   * @param britWalletId         The BRIT wallet ID
   * @param sessionKey           The session key
   * @param firstTransactionDate The first transaction date for the wallet
   *
   * @return A legacy PayerRequest with the given version number
   */
  public PayerRequest newLegacyPayerRequest(int version, BRITWalletId britWalletId, byte[] sessionKey, Optional<Date> firstTransactionDate) {

    this.britWalletId = britWalletId;
    this.sessionKey = Arrays.copyOf(sessionKey, sessionKey.length);
    return new PayerRequest(version, britWalletId, sessionKey, firstTransactionDate);

  }

  public PayerRequest newPayerRequest(BRITWalletId britWalletId, byte[] sessionKey, Optional<Date> firstTransactionDate) {

    this.britWalletId = britWalletId;
    this.sessionKey = Arrays.copyOf(sessionKey, sessionKey.length);
    return new PayerRequest(CURRENT_BRIT_VERSION, britWalletId, sessionKey, firstTransactionDate);

  }

  public EncryptedPayerRequest encryptPayerRequest(PayerRequest payerRequest) throws PayerRequestException {
    try {
      // Serialise the contents of the payerRequest
      byte[] serialisedPayerRequest = payerRequest.serialise();

      ByteArrayOutputStream encryptedBytesOutputStream = new ByteArrayOutputStream(1024);

      // TODO Can we change PGPUtils to accept a stream rather than a file to reduce IO vulnerability?
      // Make a temporary file containing the serialised payer request
      File tempFile = File.createTempFile("req", "tmp");

      // Write serialised payerRequest to the temporary file
      try (OutputStream tempStream = new FileOutputStream(tempFile)) {
        // Copy the original to the temporary location
        ByteStreams.copy(new ByteArrayInputStream(serialisedPayerRequest), tempStream);
        // Attempt to force the bits to hit the disk. In reality the OS or hard disk itself may still decide
        // to not write through to physical media for at least a few seconds, but this is the best we can do.
        tempStream.flush();
      }

      // PGP encrypt the file
      PGPUtils.encryptFile(encryptedBytesOutputStream, tempFile, payerConfig.getMatcherPublicKey());

      // TODO Secure file delete (or avoid File altogether) - consider recommendations from #295 (MultiBit Common)
      if (!tempFile.delete()) {
        throw new IOException("Could not delete file + '" + tempFile.getAbsolutePath() + "'");
      }

      return new EncryptedPayerRequest(encryptedBytesOutputStream.toByteArray());
    } catch (IOException | NoSuchProviderException | PGPException e) {
      throw new PayerRequestException("Could not encrypt PayerRequest", e);
    }
  }

  @Override
  public MatcherResponse decryptMatcherResponse(EncryptedMatcherResponse encryptedMatcherResponse, PayerRequest payerRequest) throws MatcherResponseException {

    try {

      // Get the payload
      byte[] payload = encryptedMatcherResponse.getPayload();
      if (payload == null || payload.length < 32) {
        throw new MatcherResponseException("Malformed encrypted matcher response.");
      }

      // Stretch the 20 byte britWalletId to 32 bytes (256 bits)
      byte[] stretchedBritWalletId = MessageDigest.getInstance("SHA-256").digest(britWalletId.getBytes());

      // Create an AES key from the stretchedBritWalletId and the sessionKey
      KeyParameter aesKey = new KeyParameter(stretchedBritWalletId);

      // Use the payer request version to determine how the payload is arranged
      final byte[] serialisedMatcherResponse;
      switch (payerRequest.getVersion()) {
        case 1:
          // Raw AES
          serialisedMatcherResponse = AESUtils.decrypt(payload, aesKey, sessionKey);
          // Parse the serialised MatcherResponse
          return MatcherResponse.parse(serialisedMatcherResponse);
        case 2:
          // AES with HMAC
          // Extract the HMAC (SHA256 is 32 bytes and suffixed)
          byte[] hmac = new byte[32];
          System.arraycopy(payload, payload.length - 32, hmac, 0, 32);
          // Extract the AES payload (everything up to the HMAC)
          byte[] aesPayload = new byte[payload.length - 32];
          System.arraycopy(payload, 0, aesPayload, 0, payload.length - 32);

          // Use the AES key to create the expected HMAC
          SecretKeySpec hmacKeySpec = new SecretKeySpec(aesKey.getKey(), "HmacSHA256");
          Mac mac = Mac.getInstance("HmacSHA256");
          mac.init(hmacKeySpec);
          byte[] expectedHmac = mac.doFinal(aesPayload);

          // Time-constant comparison
          if (!MessageDigest.isEqual(hmac, expectedHmac)) {
            // Log more details about the situation
            log.debug("Payload: '{}'", Utils.HEX.encode(payload));
            log.debug("AES Payload: '{}'", Utils.HEX.encode(aesPayload));
            log.debug("Actual HMAC: '{}'", Utils.HEX.encode(hmac));
            log.debug("Expect HMAC: '{}'", Utils.HEX.encode(expectedHmac));
            throw new MatcherResponseException("Invalid HMAC from server. Rejecting entire message.");
          }

          // Attempt to decrypt the AES payload
          serialisedMatcherResponse = AESUtils.decrypt(aesPayload, aesKey, sessionKey);
          // Parse the serialised MatcherResponse
          return MatcherResponse.parse(serialisedMatcherResponse);
        default:
          throw new MatcherResponseException("Unknown payer request version: " + payerRequest.getVersion());
      }

    } catch (NoSuchAlgorithmException | KeyCrypterException | MatcherResponseException | InvalidKeyException e) {
      throw new MatcherResponseException("Could not decrypt/verify MatcherResponse", e);
    }
  }

}
