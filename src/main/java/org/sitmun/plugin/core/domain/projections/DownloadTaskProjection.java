package org.sitmun.plugin.core.domain.projections;


import org.sitmun.plugin.core.domain.DownloadTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of a task.
 */
@Projection(name = "view", types = {DownloadTask.class})
public interface DownloadTaskProjection extends TaskProjection {

  @Value("#{target.format}")
  String getFormat();

  @Value("#{target.scope}")
  String getScope();
}
