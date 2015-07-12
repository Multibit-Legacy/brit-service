package org.multibit.hd.brit.rest.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.metrics.annotation.Timed;
import org.multibit.hd.brit.core.dto.EncryptedMatcherResponse;
import org.multibit.hd.brit.core.dto.EncryptedPayerRequest;
import org.multibit.hd.brit.core.dto.MatcherResponse;
import org.multibit.hd.brit.core.dto.PayerRequest;
import org.multibit.hd.brit.core.matcher.Matcher;
import org.multibit.hd.brit.rest.caches.MatcherResponseCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Resource to provide the following to application:</p>
 * <ul>
 * <li>Provision of BRIT responses</li>
 * </ul>
 *
 * @since 0.0.1
 */
@Path("/brit")
public class PublicBritResource extends BaseResource {

  private static final Logger log = LoggerFactory.getLogger(PublicBritResource.class);

  /**
   * The maximum length of the payload (typical value is 680 bytes)
   */
  private final static int MAX_PAYLOAD_LENGTH = 1500;

  private final Matcher matcher;

  private final String matcherPublicKey;

  /**
   * @param matcher The Matcher
   */
  public PublicBritResource(Matcher matcher, String matcherPublicKey) throws NoSuchAlgorithmException, IOException {

    this.matcher = matcher;
    this.matcherPublicKey = matcherPublicKey;

  }

  /**
   * Allow a Payer to compare or obtain the matcher public key
   *
   * @return A localised view containing HTML
   */
  @GET
  @Path("/public-key")
  @Consumes("text/plain")
  @Produces("text/plain")
  @Timed
  @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
  public Response getPublicKey() throws IOException {

    return Response
      .ok(matcherPublicKey)
      .build();

  }

  /**
   * Allow a Payer to submit their wallet ID as an ASCII armored payload (useful for REST clients)
   *
   * @param payload The encrypted Payer request payload
   *
   * @return The encrypted Matcher response (in binary)
   */
  @POST
  @Consumes("text/plain")
  @Produces("application/octet-stream")
  @Timed
  @CacheControl(noCache = true)
  public Response submitEncryptedPayerRequest(String payload) throws Exception {

    if (Strings.isNullOrEmpty(payload)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    return submitEncryptedPayerRequest(payload.getBytes(Charsets.UTF_8));

  }

  /**
   * Allow a Payer to submit their wallet ID as a binary payload
   *
   * @param payload The encrypted Payer request payload
   *
   * @return The encrypted Matcher response (in binary)
   */
  @POST
  @Consumes("application/octet-stream")
  @Produces("application/octet-stream")
  @Timed
  @CacheControl(noCache = true)
  public Response submitEncryptedPayerRequest(byte[] payload) {

    if (payload==null || payload.length == 0 || payload.length > MAX_PAYLOAD_LENGTH) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    EncryptedMatcherResponse encryptedMatcherResponse;
    try {
      encryptedMatcherResponse = newMatcherResponse(payload);
    } catch (NoSuchAlgorithmException e) {
      // This should never happen in a correctly configured service
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    return Response
      .created(UriBuilder.fromPath("/brit").build())
      .entity(encryptedMatcherResponse.getPayload())
      .build();

  }

  private EncryptedMatcherResponse newMatcherResponse(byte[] payload) throws NoSuchAlgorithmException {

    // Check the cache
    byte[] sha1 = MessageDigest.getInstance("SHA1").digest(payload);

    Optional<EncryptedMatcherResponse> cachedResponse = MatcherResponseCache.INSTANCE.getByPayerRequestDigest(sha1);

    if (cachedResponse.isPresent()) {
      log.debug("Using cached Matcher response");
      return cachedResponse.get();
    }

    // Must be new or uncached to be here
    log.debug("Creating Matcher response");

    EncryptedPayerRequest encryptedPayerRequest = new EncryptedPayerRequest(payload);

    final EncryptedMatcherResponse encryptedMatcherResponse;
    try {
      // The Matcher can decrypt the EncryptedPaymentRequest using its PGP secret key
      final PayerRequest payerRequest = matcher.decryptPayerRequest(encryptedPayerRequest);
      log.debug("Decrypted Version {} Payer request.", payerRequest.getVersion());

      // Get the Matcher to process the EncryptedPayerRequest
      final MatcherResponse matcherResponse = matcher.process(payerRequest);
      Preconditions.checkNotNull(matcherResponse, "'matcherResponse' must be present");

      log.debug("Created matcher response, number of addresses: {}", matcherResponse.getBitcoinAddresses().size());

      // Encrypt the Matcher response with the AES session key
      encryptedMatcherResponse = matcher.encryptMatcherResponse(matcherResponse, payerRequest);
      Preconditions.checkNotNull(encryptedMatcherResponse, "'encryptedMatcherResponse' must be present");
      log.debug("Encrypted Version {} Matcher response. Size: {} bytes", matcherResponse.getVersion(), encryptedMatcherResponse.getPayload().length);

      // Put it in the cache for later
      MatcherResponseCache.INSTANCE.put(sha1, encryptedMatcherResponse);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    Preconditions.checkState(encryptedMatcherResponse.getPayload().length > 0, "'payload' must be present");

    return encryptedMatcherResponse;
  }
}
