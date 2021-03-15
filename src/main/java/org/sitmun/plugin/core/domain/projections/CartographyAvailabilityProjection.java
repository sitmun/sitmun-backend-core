package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;
import java.util.List;

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
   * Id of the territory allowed to access to the cartography.
   */
  @Value("#{target.territory?.id}")
  String getTerritoryId();

  /**
   * Geographic code of the territory allowed to access to the cartography.
   */
  @Value("#{target.territory?.code}")
  String getTerritoryCode();

  /**
   * Name of the territory allowed to access to the cartography.
   */
  @Value("#{target.territory?.name}")
  String getTerritoryName();

  /**
   * Id of the cartography allowed to access.
   */
  @Value("#{target.cartography?.id}")
  Integer getCartographyId();

  /**
   * Name of the cartography allowed to access.
   */
  @Value("#{target.cartography?.name}")
  String getCartographyName();

  /**
   * Layers of the cartography allowed to access.
   */
  @Value("#{target.cartography?.layers}")
  List<String> getCartographyLayers();

}
