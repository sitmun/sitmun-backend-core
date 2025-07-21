package org.sitmun.domain.user.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/** Projections for REST views of user configuration. */
@Projection(name = "view", types = UserConfiguration.class)
public interface UserConfigurationProjection {

  /** User configuration identifier. */
  @Value("#{target.id}")
  Integer getId();

  /** Username of the user. */
  @Value("#{target.user?.username}")
  String getUser();

  /** User identifier. */
  @JsonProperty("userId")
  @Value("#{target.user?.id}")
  Integer getUserId();

  /** Territory name. */
  @Value("#{target.territory?.name}")
  String getTerritory();

  /** Territory identifier. */
  @JsonProperty("territoryId")
  @Value("#{target.territory?.id}")
  Integer getTerritoryId();

  /** Role name. */
  @Value("#{target.role?.name}")
  String getRole();

  /** Role identifier. */
  @JsonProperty("roleId")
  @Value("#{target.role?.id}")
  Integer getRoleId();

  /** Role applies to children territories. */
  @Value("#{target.appliesToChildrenTerritories}")
  Boolean getAppliesToChildrenTerritories();

  /** Creation date. */
  @Value("#{target.createdDate}")
  Date getCreatedDate();
}
