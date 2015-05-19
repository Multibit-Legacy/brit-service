package org.multibit.hd.brit_server.health;

/**
 * <p>HealthCheck to provide the following to application:</p>
 * <ul>
 * <li>Verifies that BRIT service can round trip a request correctly</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */

import com.sun.jersey.api.client.Client;
import com.yammer.metrics.core.HealthCheck;

import javax.ws.rs.core.MediaType;

public class BritPublicKeyHealthCheck extends HealthCheck {

  public BritPublicKeyHealthCheck() {
    super("BRIT public key health check");
  }

  @Override
  protected Result check() throws Exception {

    // Send the encrypted request to the Matcher
    Client client = new Client();
    String actualResponse = client
      .resource("http://localhost:7070/brit/public-key")
      .accept(MediaType.TEXT_PLAIN_TYPE)
      .get(String.class);
    if (actualResponse.contains("mQENBFMxpmwBCADAypURRQTJuxAk1CcTVE5fg3vFmts8O2+VQwILCkhJHJ1wwZEO")) {
      return Result.unhealthy("Public key is TEST key. Check /var/brit/matcher contents carefully.");
    }

    // Must be OK to be here
    return Result.healthy();
  }

}