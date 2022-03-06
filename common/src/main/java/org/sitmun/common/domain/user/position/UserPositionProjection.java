package org.sitmun.common.domain.user.position;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * Projections for REST views of user position.
 */
@Projection(name = "view", types = {UserPosition.class})
public interface UserPositionProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Position description.
   */
  @Value("#{target.name}")
  String getName();

  /**
   * Organization.
   */
  @Value("#{target.organization}")
  String getOrganization();

  /**
   * Email.
   */
  @Value("#{target.email}")
  String getEmail();

  /**
   * Creation date.
   */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /**
   * Expiration date.
   */
  @Value("#{target.expirationDate}")
  Date getExpirationDate();

  /**
   * Type of user (only used in some cases).
   */
  @Value("#{target.type}")
  String getType();

  /**
   * Territory name.
   */
  @Value("#{target.territory?.name}")
  String getTerritoryName();

  /**
   * User id.
   */
  @Value("#{target.user?.id}")
  Integer getUserId();
}
