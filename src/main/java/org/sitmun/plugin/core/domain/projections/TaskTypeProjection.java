package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Map;

/**
 * Projections for REST views of a task type.
 */
@Projection(name = "view", types = {TaskType.class})
public interface TaskTypeProjection {

  @Value("#{target.id}")
  Long getId();

  /**
   * Task type name.
   */
  @Value("#{target.name}")
  String getName();

  /**
   * Task type title.
   */
  @Value("#{target.title}")
  String getTitle();

  /**
   * Is active.
   */
  @Value("#{target.enabled}")
  Boolean getEnabled();

  /**
   * Task type parent.
   */
  @Value("#{target.parent?.id}")
  Long getParentId();

  /**
   * Order in the UI.
   */
  @Value("#{target.folder}")
  Boolean isFolder();

  /**
   * Order in the UI.
   */
  @Value("#{target.order}")
  Integer getOrder();

  /**
   * Task type specification
   */
  @Value("#{target.specification}")
  Map<String, Object> getSpecification();
}
