package org.multibit.site.resources;

import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.metrics.annotation.Timed;
import org.multibit.site.utils.StreamUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class PublicBritResource extends BaseResource {

  /**
   * Allow a Payer to compare or obtain the matcher public key
   *
   * @return A localised view containing HTML
   */
  @GET
  @Path("/public-key")
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
   * @return A localised view containing HTML
   */
  @POST
  @Timed
  @CacheControl(noCache = true)
  public Response submitWalletId(String message) {


    return Response
      .created(UriBuilder.fromPath("/brit").build())
      .entity(message)
      .build();

  }

}
