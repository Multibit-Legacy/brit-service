package org.multibit.hd.brit.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * <p>DropWizard Configuration to provide the following to application:</p>
 * <ul>
 * <li>Initialisation code</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class BritConfiguration extends Configuration {

  @NotEmpty
  @JsonProperty
  private String matcherStoreDirectory ="/var/brit/matcher";

  @Valid
  @NotNull
  @JsonProperty
  private boolean production = true;

  public String getMatcherStoreDirectory() {
    return matcherStoreDirectory;
  }

  public boolean isProduction() {
    return production;
  }
}
