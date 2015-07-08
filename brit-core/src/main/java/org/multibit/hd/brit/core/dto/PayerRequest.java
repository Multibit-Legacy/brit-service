package org.multibit.hd.brit.core.dto;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import org.bitcoinj.core.Utils;
import org.multibit.hd.brit.core.exceptions.PayerRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Strings;

import java.util.Arrays;
import java.util.Date;

/**
 * <p>DTO to provide the following to BRIT API:</p>
 * <ul>
 * <li>The unencrypted version of the message sent by the Payer to the Matcher</li>
 * <li>Typically 'encrypt' is called and the EncryptedPayerRequest is actually sent on the wire</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class PayerRequest {

  private static final Logger log = LoggerFactory.getLogger(PayerRequest.class);

  public static final char SEPARATOR = '\n';
  public static final String OPTIONAL_NOT_PRESENT_TEXT = "not-present";

  private final int version;
  private final BRITWalletId britWalletId;
  private final byte[] sessionKey;
  private final Optional<Date> firstTransactionDate;

  public PayerRequest(int version, BRITWalletId britWalletId, byte[] sessionKey, Optional<Date> firstTransactionDate) {
    this.version = version;
    this.britWalletId = britWalletId;
    this.sessionKey = Arrays.copyOf(sessionKey, sessionKey.length);
    this.firstTransactionDate = firstTransactionDate;
  }

  /**
   * @return The BRIT protocol version
   */
  public int getVersion() {
    return version;
  }

  /**
   * @return The session key
   */
  public byte[] getSessionKey() {
    return Arrays.copyOf(sessionKey, sessionKey.length);
  }

  /**
   * @return The BRIT wallet ID
   */
  public BRITWalletId getBritWalletId() {
    return britWalletId;
  }

  /**
   * @return The first transaction date (if present)
   */
  public Optional<Date> getFirstTransactionDate() {
    return firstTransactionDate;
  }

  /**
   * <p>Serialise the contents of the PayerRequest to a byte stream</p>
   * <p>(This is not very efficient but the intermediate string is human readable)</p>
   *
   * @return Bytes representing the PayerRequest
   */
  public byte[] serialise() {

    StringBuilder builder = new StringBuilder()
      .append(getVersion())
      .append(SEPARATOR)
      .append(Utils.HEX.encode(britWalletId.getBytes()))
      .append(SEPARATOR)
      .append(Utils.HEX.encode(sessionKey))
      .append(SEPARATOR);

    if (firstTransactionDate.isPresent()) {
      builder.append(firstTransactionDate.get().getTime());
    } else {
      builder.append(OPTIONAL_NOT_PRESENT_TEXT);
    }

    return builder.toString().getBytes(Charsets.UTF_8);
  }

  /**
   * <p>Parse the serialised Payer request</p>
   *
   * @param serialisedPayerRequest The serialised payer request
   *
   * @return The Payer request
   */
  public static PayerRequest parse(byte[] serialisedPayerRequest) {

    String serialisedPaymentRequestAsString = new String(serialisedPayerRequest, Charsets.UTF_8);

    String[] rows = Strings.split(serialisedPaymentRequestAsString, SEPARATOR);
    if (rows.length == 4) {
      // Extract version
      final int version;
      try {
        version = Integer.parseInt(rows[0]);
      } catch (IllegalArgumentException e) {
        throw new PayerRequestException("The serialisedPayerRequest had a malformed version entry");
      }
      // Version check
      if (version < 1 || version > 2) {
        throw new PayerRequestException("The serialisedPayerRequest had a version of '" + rows[0] + "'. This code only understands the range [1,2]");
      }

      final BRITWalletId britWalletId = new BRITWalletId(rows[1]);
      final byte[] sessionKey = Utils.parseAsHexOrBase58(rows[2]);
      final Optional<Date> firstTransactionDateOptional;

      if (OPTIONAL_NOT_PRESENT_TEXT.equals(rows[3])) {
        firstTransactionDateOptional = Optional.absent();
      } else {
        firstTransactionDateOptional = Optional.of(new Date(Long.parseLong(rows[3])));
      }

      log.debug("Parsed OK");
      return new PayerRequest(version, britWalletId, sessionKey, firstTransactionDateOptional);

    } else {
      throw new PayerRequestException("Expected 4 rows of data. Found " + rows.length);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PayerRequest that = (PayerRequest) o;

    if (version != that.version) return false;
    if (!britWalletId.equals(that.britWalletId)) return false;
    if (!Arrays.equals(sessionKey, that.sessionKey)) return false;
    return firstTransactionDate.equals(that.firstTransactionDate);

  }

  @Override
  public int hashCode() {
    int result = version;
    result = 31 * result + britWalletId.hashCode();
    result = 31 * result + Arrays.hashCode(sessionKey);
    result = 31 * result + firstTransactionDate.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "PayerRequest{" +
      "version=" + version +
      ", britWalletId=" + britWalletId +
      ", sessionKey=" + Arrays.toString(sessionKey) +
      ", firstTransactionDate=" + firstTransactionDate +
      '}';
  }
}
