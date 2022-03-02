package org.sitmun.domain.projections;

import org.sitmun.common.types.envelope.Envelope;
import org.sitmun.common.types.point.Point;
import org.sitmun.domain.Territory;
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
  @Deprecated
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
  @Deprecated
  Integer getGroupTypeId();

  /**
   * Group type name.
   */
  @Value("#{target.groupType?.name}")
  @Deprecated
  String getGroupTypeName();

  /**
   * Type identifier.
   */
  @Value("#{target.type?.id}")
  Integer getTypeId();

  /**
   * Type name.
   */
  @Value("#{target.type?.name}")
  String getTypeName();


  /**
   * Type is top.
   */
  @Value("#{target.type?.topType}")
  Boolean getTypeTopType();

  /**
   * Type is bottom.
   */
  @Value("#{target.type?.bottomType}")
  Boolean getTypeBottomType();


  /**
   * Center of the territory.
   */
  @Value("#{target.center}")
  Point getCenter();

  /**
   * Default zoom level.
   */
  @Value("#{target.defaultZoomLevel}")
  Integer getDefaultZoomLevel();

}
