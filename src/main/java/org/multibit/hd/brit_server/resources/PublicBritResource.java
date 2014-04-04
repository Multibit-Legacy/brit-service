package org.multibit.hd.brit_server.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.metrics.annotation.Timed;
import org.multibit.hd.brit.dto.EncryptedMatcherResponse;
import org.multibit.hd.brit.dto.EncryptedPayerRequest;
import org.multibit.hd.brit.dto.MatcherResponse;
import org.multibit.hd.brit.dto.PayerRequest;
import org.multibit.hd.brit.matcher.Matcher;
import org.multibit.hd.brit_server.utils.StreamUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

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

  private final Matcher matcher;

  /**
   * @param matcher The Matcher
   */
  public PublicBritResource(Matcher matcher) {
    this.matcher = matcher;
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
  @CacheControl(noCache = true)
  public Response getPublicKey() throws IOException {

    // TODO Implement in-memory caching of this
    String matcherPublicKey = StreamUtils.toString(PublicBritResource.class.getResourceAsStream("/brit/matcher-pubkey.asc"));

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

    EncryptedMatcherResponse encryptedMatcherResponse = newMatcherResponse(payload.getBytes(Charsets.UTF_8));

    return Response
      .created(UriBuilder.fromPath("/brit").build())
      .entity(encryptedMatcherResponse.getPayload())
      .build();

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

    EncryptedMatcherResponse encryptedMatcherResponse = newMatcherResponse(payload);

    return Response
      .created(UriBuilder.fromPath("/brit").build())
      .entity(encryptedMatcherResponse.getPayload())
      .build();

  }

  public EncryptedMatcherResponse newMatcherResponse(byte[] payload) {

    EncryptedPayerRequest encryptedPayerRequest = new EncryptedPayerRequest(payload);

    final EncryptedMatcherResponse encryptedMatcherResponse;
    try {
      // The Matcher can decrypt the EncryptedPaymentRequest using its PGP secret key
      final PayerRequest matcherPayerRequest = matcher.decryptPayerRequest(encryptedPayerRequest);

      // Get the matcher to process the EncryptedPayerRequest
      final MatcherResponse matcherResponse = matcher.process(matcherPayerRequest);
      Preconditions.checkNotNull(matcherResponse, "'matcherResponse' must be present");

      // Encrypt the MatcherResponse with the AES session key
      encryptedMatcherResponse = matcher.encryptMatcherResponse(matcherResponse);
      Preconditions.checkNotNull(encryptedMatcherResponse, "'encryptedMatcherResponse' must be present");

    } catch (Exception e) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    return encryptedMatcherResponse;


  }

}
