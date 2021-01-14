package org.sitmun.plugin.core.domain.projections;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * Projections for REST views of an application.
 */
@Projection(name = "view", types = {CartographyAvailability.class})
public interface CartographyAvailabilityProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Creation date.
   */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /**
   * Owner of the Geographic Information.
   * Keeps the owner's name when ownership is not obvious or is an exception.
   */
  @Value("#{target.owner}")
  String getOwner();

  /**
   * Identifier of the territory allowed to access to the cartography.
   */
  @Value("#{target.territory.id}")
  @JsonProperty("territory.id")
  Integer getTerritoryId();

  /**
   * Name of the territory allowed to access to the cartography.
   */
  @Value("#{target.territory.name}")
  @JsonProperty("territoryName")
  String getTerritoryName();

}
