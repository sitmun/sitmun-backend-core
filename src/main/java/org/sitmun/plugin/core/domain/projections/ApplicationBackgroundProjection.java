package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of an application background.
 */
@Projection(name = "view", types = {ApplicationBackground.class})
public interface ApplicationBackgroundProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Order of preference.
   * It can be used for sorting the list of backgrounds in a view.
   */
  @Value("#{target.order}")
  Integer getOrder();

  /**
   * Background name.
   */
  @Value("#{target.background != null ? target.background.name : null}")
  String getBackgroundName();

  /**
   * Background description.
   */
  @Value("#{target.background != null ? target.background.description : null}")
  String getBackgroundDescription();
}
