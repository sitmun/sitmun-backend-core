package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.Background;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "backgrounds", path = "backgrounds")
public interface BackgroundRepository extends CrudRepository<Background, BigInteger> {
  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  Background save(@P("entity") Background entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Background entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Background> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background', 'read')")
  Background findOne(@P("entityId") BigInteger entityId);

}