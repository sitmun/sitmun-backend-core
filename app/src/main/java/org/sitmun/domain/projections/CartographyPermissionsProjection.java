package org.sitmun.domain.projections;

import org.sitmun.domain.CartographyPermission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.List;

/**
 * Projections for REST views of an application.
 */
@Projection(name = "view", types = {CartographyPermission.class})
public interface CartographyPermissionsProjection {

  @Value("#{target.id}")
  Integer getId();

  @Value("#{target.type}")
  String getType();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.roles.![name]}")
  List<String> getRoleNames();

}
