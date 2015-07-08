package org.multibit.hd.brit.core.payer;

import com.google.common.base.Optional;
import org.multibit.hd.brit.core.dto.*;
import org.multibit.hd.brit.core.exceptions.MatcherResponseException;
import org.multibit.hd.brit.core.exceptions.PayerRequestException;

import java.util.Date;

/**
 * <p>Interface to provide the following to BRIT:</p>
 * <ul>
 * <li>Encapsulation of functionality required to make BRIT payments</li>
 * </ul>
 *
 * @since 0.0.1
 */
public interface Payer {

  /**
   * Get the PayerConfig, which contains the Matcher's public PGP key
   *
   * @return A Payer configuration
   */
  PayerConfig getConfig();

  /**
   * Create an unencrypted PayerRequest for transmission to the Matcher
   *
   * @param britWalletId         The britWalletId of the Payer's wallet
   * @param sessionKey           A random sessionKey
   * @param firstTransactionDate The date of the first transaction in the Payer's wallet, or Optional.absent() if there are none.
   *
   * @return A new Payer request (unencrypted) based the current BRIT version
   */
  PayerRequest newPayerRequest(BRITWalletId britWalletId, byte[] sessionKey, Optional<Date> firstTransactionDate);

  /**
   * Encrypt the PayerRequest with the Matcher public PGP key
   *
   * @param payerRequest The PayerRequest to encrypt
   *
   * @return the EncryptedPayerRequest containing the encrypted payload
   *
   * @throws PayerRequestException
   */
  EncryptedPayerRequest encryptPayerRequest(PayerRequest payerRequest) throws PayerRequestException;

  /**
   * Decrypt the encryptedMatcherResponse using an AES key derived from the BRITWalletId and sessionKey
   *
   * @param encryptedMatcherResponse The encrypted Matcher response
   * @param payerRequest             The Payer request (providing the version)
   *
   * @return A Matcher response (unencrypted)
   *
   * @throws MatcherResponseException
   */
  MatcherResponse decryptMatcherResponse(EncryptedMatcherResponse encryptedMatcherResponse, PayerRequest payerRequest) throws MatcherResponseException;

}
