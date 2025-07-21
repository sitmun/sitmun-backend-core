package org.sitmun.domain.cartography.permission;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/** Projections for REST views of an application. */
@Projection(name = "view", types = CartographyPermission.class)
public interface CartographyPermissionProjection {

  @Value("#{target.id}")
  Integer getId();

  @Value("#{target.type}")
  String getType();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.roles.![name]}")
  List<String> getRoleNames();
}
