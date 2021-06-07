package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.Envelope;
import org.sitmun.plugin.core.domain.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * Projections for REST views of a territory.
 */
@Projection(name = "view", types = {Territory.class})
public interface TerritoryProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Geographic code.
   */
  @Value("#{target.code}")
  String getCode();

  /**
   * Territory name.
   */
  @Value("#{target.name}")
  String getName();

  /**
   * Territorial authority name.
   */
  @Value("#{target.territorialAuthorityName}")
  String getTerritorialAuthorityName();

  /**
   * Territorial authority address.
   */
  @Value("#{target.territorialAuthorityAddress}")
  String getTerritorialAuthorityAddress();

  /**
   * Territorial authority email.
   */
  @Value("#{target.territorialAuthorityEmail}")
  String getTerritorialAuthorityEmail();

  /**
   * Territory scope.
   */
  @Value("#{target.scope}")
  String getScope();

  /**
   * Link to the territorial authority logo.
   */
  @Value("#{target.territorialAuthorityLogo}")
  String getTerritorialAuthorityLogo();

  /**
   * Bounding box of the territory.
   */
  @Value("#{target.extent}")
  Envelope getExtent();

  /**
   * <code>true</code> if the territory is blocked.
   */
  @Value("#{target.blocked}")
  Boolean getBlocked();

  /**
   * Notes.
   */
  @Value("#{target.note}")
  String getNote();

  /**
   * Creation date.
   */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /**
   * Group type identifier.
   */
  @Value("#{target.groupType?.id}")
  Integer getGroupTypeId();

  /**
   * Group type name.
   */
  @Value("#{target.groupType?.name}")
  String getGroupTypeName();
}
