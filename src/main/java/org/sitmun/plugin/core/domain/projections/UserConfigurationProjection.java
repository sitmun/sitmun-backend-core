package org.sitmun.plugin.core.domain.projections;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of user configuration.
 */
@Projection(name = "view", types = {UserConfiguration.class})
public interface UserConfigurationProjection {

  /**
   * User configuration identifier.
   */
  @Value("#{target.id}")
  Integer getId();

  /**
   * Username of the user.
   */
  @Value("#{target.user.username}")
  String getUser();

  /**
   * User identifier.
   */
  @JsonProperty("userId")
  @Value("#{target.user.id}")
  Integer getUserId();

  /**
   * Territory name.
   */
  @Value("#{target.territory.name}")
  String getTerritory();

  /**
   * Territory identifier.
   */
  @JsonProperty("territoryId")
  @Value("#{target.territory.id}")
  Integer getTerritoryId();

  /**
   * Role name.
   */
  @Value("#{target.role.name}")
  String getRole();

  /**
   * Role identifier.
   */
  @JsonProperty("roleId")
  @Value("#{target.role.id}")
  Integer getRoleId();

  /**
   * Role children name or {@code null} if not set.
   */
  @Value("#{target.roleChildren != null ? target.roleChildren.name : null}")
  String getRoleChildren();
}
