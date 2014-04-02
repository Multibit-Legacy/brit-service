package org.multibit.site.resources;

import com.google.common.base.Preconditions;
import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.metrics.annotation.Timed;
import org.multibit.hd.brit.dto.EncryptedMatcherResponse;
import org.multibit.hd.brit.dto.EncryptedPayerRequest;
import org.multibit.hd.brit.dto.MatcherResponse;
import org.multibit.hd.brit.dto.PayerRequest;
import org.multibit.hd.brit.matcher.Matcher;
import org.multibit.site.utils.StreamUtils;

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
  @Produces("text/plain")
  @Consumes("text/plain")
  @Timed
  @CacheControl(noCache = true)
  public Response getPublicKey() throws IOException {

    String matcherPublicKey = StreamUtils.toString(PublicBritResource.class.getResourceAsStream("/brit/matcher-pubkey.asc"));

    return Response
      .ok(matcherPublicKey)
      .build();

  }

  /**
   * Allow a Payer to submit their wallet ID
   *
   * @param payload The encrypted Payer request payload
   *
   * @return A localised view containing HTML
   */
  @POST
  @Produces("application/octet-stream")
  @Consumes("application/octet-stream")
  @Timed
  @CacheControl(noCache = true)
  public Response submitWalletId(byte[] payload) throws Exception {

    EncryptedPayerRequest encryptedPayerRequest = new EncryptedPayerRequest(payload);

    // The Matcher can decrypt the EncryptedPaymentRequest using its PGP secret key
    PayerRequest matcherPayerRequest = matcher.decryptPayerRequest(encryptedPayerRequest);

    // Get the matcher to process the EncryptedPayerRequest
    MatcherResponse matcherResponse = matcher.process(matcherPayerRequest);
    Preconditions.checkNotNull(matcherResponse,"'matcherResponse' must be present");

    // Encrypt the MatcherResponse with the AES session key
    EncryptedMatcherResponse encryptedMatcherResponse = matcher.encryptMatcherResponse(matcherResponse);
    Preconditions.checkNotNull(encryptedMatcherResponse,"'encryptedMatcherResponse' must be present");

    return Response
      .created(UriBuilder.fromPath("/brit").build())
      .entity(encryptedMatcherResponse.getPayload())
      .build();

  }

}
