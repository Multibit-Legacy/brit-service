package org.multibit.hd.brit_server.caches;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

/**
 * <p>Cache to provide the following to resources:</p>
 * <ul>
 * <li>In-memory thread-safe cache for long-lived artifacts generated periodically</li>
 * </ul>
 * <p>This protects against a single IP address hammering the BRIT server</p>
 *
 * @since 0.0.1
 */
public enum AddressThrottlingCache {

  // Provide a global singleton for the application
  INSTANCE;

  // A lot of threads will hit this cache
  private volatile Cache<String, DateTime> addressCache;

  AddressThrottlingCache() {
    reset();
  }

  /**
   * Resets the cache
   */
  public AddressThrottlingCache reset() {

    // Build the cache
    if (addressCache != null) {
      addressCache.invalidateAll();
    }

    // Store a few items permanently
    addressCache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(1, TimeUnit.MINUTES)
      .maximumSize(10_000)
      .build();

    return INSTANCE;
  }

  /**
   * @param remoteAddress The IP address of the remote client
   *
   * @return True if the result of the put was to create a new entry
   */
  public boolean put(String remoteAddress) {

    Preconditions.checkNotNull(remoteAddress);

    boolean isNew = addressCache.getIfPresent(remoteAddress) == null;
    if (isNew) {
      // Create the entry and report that we created it
      addressCache.put(remoteAddress, DateTime.now(DateTimeZone.UTC));
    }

    return isNew;
  }

}
