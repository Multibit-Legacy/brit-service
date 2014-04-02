package org.multibit.site.resources;

import com.yammer.dropwizard.testing.ResourceTest;
import org.junit.Test;
import org.multibit.site.testing.FixtureAsserts;

import javax.ws.rs.core.MediaType;

public class PublicBritResourceTest extends ResourceTest {

  private final PublicBritResource testObject=new PublicBritResource();

  @Override
  protected void setUpResources() {

    // Configure resources
    addResource(testObject);

  }

  @Test
  public void GET_MatcherPublicKey() throws Exception {

    // Build the request
    String actualResponse = client()
      .resource("/brit/public-key")
      .header("Content-Type","text/plain")
      .accept(MediaType.TEXT_PLAIN_TYPE)
      .get(String.class);

    FixtureAsserts.assertStringMatchesStringFixture(
      "Payer submits wallet info",
      actualResponse,
      "/brit/matcher-pubkey.asc"
    );

  }

  @Test
  public void POST_PayerWalletInfo() throws Exception {

    // Build the request
    String actualResponse = client()
      .resource("http://localhost:8080/brit")
      .header("Content-Type","text/plain")
      .accept(MediaType.TEXT_PLAIN_TYPE)
      .entity("Hello")
      .post(String.class);

    FixtureAsserts.assertStringMatchesStringFixture(
      "Payer submits wallet info",
      actualResponse,
      "/fixtures/brit/payer-1.txt"
    );

  }


}
