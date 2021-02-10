package org.sitmun.plugin.core.domain.projections;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.sitmun.plugin.core.domain.Background;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * Projections for REST views of a background.
 */
@Projection(name = "view", types = {Background.class})
public interface BackgroundProjection {

  @Value("#{target.id}")
  Long getId();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.description}")
  String getDescription();

  @Value("#{target.active}")
  Boolean getActive();

  @Value("#{target.createdDate}")
  Date getCreatedDate();

  @Value("#{target.cartographyGroup != null? target.cartographyGroup.name : null}")
  String getCartographyGroupName();

  @Value("#{target.cartographyGroup != null? target.cartographyGroup.id : null}")
  @JsonProperty("cartographyGroupId")
  Long getCartographyGroupId();
}
