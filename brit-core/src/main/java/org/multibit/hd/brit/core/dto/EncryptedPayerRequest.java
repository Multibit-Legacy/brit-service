package org.multibit.hd.brit.core.dto;

import java.util.Arrays;

/**
 * <p>DTO to provide the following to Payer and Matcher:</p>
 * <ul>
 * <li>PGP encrypted version of PayerRequest</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class EncryptedPayerRequest {
  /**
   * The encrypted payload
   */
  private byte[] payload;

  public EncryptedPayerRequest(byte[] payload) {
    this.payload = Arrays.copyOf(payload, payload.length);
  }

  /**
   * @return The payload
   */
  public byte[] getPayload() {
    return Arrays.copyOf(payload, payload.length);
  }

  @Override
  public String toString() {
    return "EncryptedPayerRequest{" +
      "payload=" + Arrays.toString(payload) +
      '}';
  }
}
