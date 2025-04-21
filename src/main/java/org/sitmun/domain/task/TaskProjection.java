package org.sitmun.domain.task;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;
import java.util.Map;

/**
 * Projections for REST views of a task.
 */
@Projection(name = "view", types = Task.class)
public interface TaskProjection {

  @Value("#{target.id}")
  Integer getId();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.createdDate}")
  Date getCreatedDate();

  @Value("#{target.order}")
  Integer getOrder();

  @Value("#{target.group?.name }")
  String getGroupName();

  @Value("#{target.group?.id}")
  Integer getGroupId();

  @Value("#{target.ui?.id}")
  Integer getUiId();

  @Value("#{target.properties}")
  Map<String, Object> getProperties();

  @Value("#{target.service?.id}")
  Integer getServiceId();

  @Value("#{target.service?.name}")
  String getServiceName();

  @Value("#{target.cartography?.id}")
  Integer getCartographyId();

  @Value("#{target.cartography?.name}")
  String getCartographyName();

  @Value("#{target.connection?.id}")
  Integer getConnectionId();

  @Value("#{target.connection?.name}")
  String getConnectionName();

  @Value("#{target.type?.id}")
  Integer getTypeId();

  @Value("#{target.type?.name}")
  String getTypeName();
}
