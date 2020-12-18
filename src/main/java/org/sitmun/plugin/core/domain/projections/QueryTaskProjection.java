package org.sitmun.plugin.core.domain.projections;


import org.sitmun.plugin.core.domain.QueryTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of a task.
 */
@Projection(name = "view", types = {QueryTask.class})
public interface QueryTaskProjection extends TaskProjection {

  @Value("#{target.command}")
  String getCommand();

  @Value("#{target.scope}")
  String getScope();
}
