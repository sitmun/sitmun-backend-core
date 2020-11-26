package org.sitmun.plugin.core.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of user configuration.
 */
@Projection(name = "view", types = {UserConfiguration.class})
public interface UserConfigurationProjection {

  /**
   * Username of the user.
   */
  @Value("#{target.user.username}")
  String getUser();

  /**
   * Territory name.
   */
  @Value("#{target.territory.name}")
  String getTerritory();

  /**
   * Role name.
   */
  @Value("#{target.role.name}")
  String getRole();

  /**
   * Role children name or {@code null} if not set.
   */
  @Value("#{target.roleChildren != null ? target.roleChildren.name : null}")
  String getRoleChildren();
}
