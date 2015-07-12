package org.multibit.hd.brit.rest.caches;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.multibit.hd.brit.core.dto.EncryptedMatcherResponse;

import java.util.concurrent.TimeUnit;

/**
 * <p>Cache to provide the following to resources:</p>
 * <ul>
 * <li>In-memory thread-safe cache for page view instances that may expire</li>
 * </ul>
 * <p>This protects against a multiple IP addresses hammering the BRIT server for a single WalletId</p>
 *
 * @since 0.0.1
 */
public enum MatcherResponseCache {

  // Provide a global singleton for the application
  INSTANCE;

  // A lot of threads will hit this cache
  private volatile Cache<byte[], EncryptedMatcherResponse> pageCache;

  MatcherResponseCache() {
    reset();
  }

  /**
   * Resets the cache
   */
  public MatcherResponseCache reset() {

    // Build the cache
    if (pageCache != null) {
      pageCache.invalidateAll();
    }

    // Provide a simple protection against periods of high activity
    // while allowing a developer to make progress with changes
    pageCache = CacheBuilder
      .newBuilder()
      .maximumSize(10_000)
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .build();

    return INSTANCE;
  }

  /**
   * @param payerRequestDigest       The encrypted Payer request digest (usually SHA1)
   * @param encryptedMatcherResponse The encrypted Matcher response
   */
  public void put(byte[] payerRequestDigest, EncryptedMatcherResponse encryptedMatcherResponse) {

    Preconditions.checkNotNull(encryptedMatcherResponse, "'encryptedMatcherResponse' must be present");
    Preconditions.checkNotNull(payerRequestDigest, "'payerRequestDigest' must be present");

    pageCache.put(payerRequestDigest, encryptedMatcherResponse);
  }

  /**
   * @param payerRequestDigest The encrypted Payer request digest (usually SHA1)
   *
   * @return The encrypted Matcher response if present
   */
  public Optional<EncryptedMatcherResponse> getByPayerRequestDigest(byte[] payerRequestDigest) {

    // Check the cache
    Optional<EncryptedMatcherResponse> responseOptional = Optional.fromNullable(pageCache.getIfPresent(payerRequestDigest));

    if (responseOptional.isPresent()) {
      // Ensure we refresh the cache on a check to maintain the session timeout
      pageCache.put(payerRequestDigest, responseOptional.get());
    }

    return responseOptional;
  }

}
