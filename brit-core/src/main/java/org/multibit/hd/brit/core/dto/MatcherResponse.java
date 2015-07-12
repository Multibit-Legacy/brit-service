package org.multibit.hd.brit.core.dto;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.params.MainNetParams;
import org.multibit.hd.brit.core.exceptions.MatcherResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Strings;

import java.util.Date;
import java.util.Set;

/**
 * <p>DTO to provide the following to BRIT API:</p>
 * <ul>
 * <li>The response message from the Matcher to the Payer</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class MatcherResponse {

  private static final Logger log = LoggerFactory.getLogger(PayerRequest.class);

  public static final String OPTIONAL_NOT_PRESENT_TEXT = "not-present";

  private final int version;
  private final Optional<Date> replayDateOptional;
  private final Set<Address> bitcoinAddresses;

  /**
   * @param version            The BRIT version
   * @param replayDateOptional The replay date
   * @param bitcoinAddresses   The Bitcoin addresses to use
   */
  public MatcherResponse(int version, Optional<Date> replayDateOptional, Set<Address> bitcoinAddresses) {
    this.version = version;
    this.replayDateOptional = replayDateOptional;
    this.bitcoinAddresses = bitcoinAddresses;
  }

  /**
   * @return The BRIT protocol version
   */
  public int getVersion() {
    return version;
  }

  public Set<Address> getBitcoinAddresses() {
    return bitcoinAddresses;
  }

  public Optional<Date> getReplayDate() {
    return replayDateOptional;
  }

  /**
   * Serialise a MatcherResponse
   * Format is:
   * versionNumber
   * replayDate
   * address1
   * ...
   * addressN
   */
  public byte[] serialise() {
    StringBuilder builder = new StringBuilder();
    builder.append(getVersion()).append(PayerRequest.SEPARATOR);
    if (replayDateOptional.isPresent()) {
      builder.append(replayDateOptional.get().getTime()).append(PayerRequest.SEPARATOR);
    } else {
      builder.append(OPTIONAL_NOT_PRESENT_TEXT).append(PayerRequest.SEPARATOR);
    }
    if (bitcoinAddresses != null) {
      for (Address address : bitcoinAddresses) {
        builder.append(address).append(PayerRequest.SEPARATOR);
      }
    }

    return builder.toString().getBytes(Charsets.UTF_8);

  }

  /**
   * Parse a serialised MatcherResponse
   *
   * @param serialisedMatcherResponse te serialised MatcherResponse
   *
   * @return a recreated MatcherResponse
   */
  public static MatcherResponse parse(byte[] serialisedMatcherResponse) throws MatcherResponseException {

    String serialisedMatcherResponseAsString = new String(serialisedMatcherResponse, Charsets.UTF_8);

    log.trace("Attempting to parse matcher response:\n{}", serialisedMatcherResponseAsString);
    String[] rows = Strings.split(serialisedMatcherResponseAsString, '\n');

    if (rows.length > 0) {
      // Extract version
      final int version;
      try {
        version = Integer.parseInt(rows[0]);
      } catch (IllegalArgumentException e) {
        throw new MatcherResponseException("The serialisedPayerRequest had a malformed version entry");
      }
      // Version check
      if (version < 1 || version > 2) {
        throw new MatcherResponseException("The serialisedPayerRequest had a version of '" + rows[0] + "'. This code only understands the range [1,2]");
      }

      Optional<Date> replayDateOptional;
      if (OPTIONAL_NOT_PRESENT_TEXT.equals(rows[1])) {
        replayDateOptional = Optional.absent();
      } else {
        replayDateOptional = Optional.of(new Date(Long.parseLong(rows[1])));
      }
      Set<Address> bitcoinAddresses = Sets.newHashSet();
      if (rows.length > 2) {
        for (int i = 2; i < rows.length; i++) {
          if (rows[i] != null && rows[i].length() > 0) {
            try {
              bitcoinAddresses.add(new Address(MainNetParams.get(), rows[i]));
            } catch (AddressFormatException e) {
              // We ignore the malformed address but continue processing for the following reasons:
              // 1. With a valid HMAC it is overwhelmingly likely to be a glitch at the server so safe to ignore
              // 2. Coercing this code to send Version 1 means it is already compromised
              // 3. Version 1 has this behaviour so this code works with legacy PayerRequests assisting testing
              log.warn("Malformed address in response: '{}'", rows[i]);
            }
          }
        }
      }

      return new MatcherResponse(version, replayDateOptional, bitcoinAddresses);

    } else {
      throw new MatcherResponseException("Cannot parse the response. Require 2 or more rows.");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MatcherResponse that = (MatcherResponse) o;

    if (bitcoinAddresses != null ? !bitcoinAddresses.equals(that.bitcoinAddresses) : that.bitcoinAddresses != null) return false;
    if (replayDateOptional != null ? !replayDateOptional.equals(that.replayDateOptional) : that.replayDateOptional != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = replayDateOptional != null ? replayDateOptional.hashCode() : 0;
    result = 31 * result + (bitcoinAddresses != null ? bitcoinAddresses.hashCode() : 0);
    return result;
  }

}
