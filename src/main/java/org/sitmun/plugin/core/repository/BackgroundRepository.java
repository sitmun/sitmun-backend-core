package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Background;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@RepositoryRestResource(collectionResourceRel = "backgrounds", path = "backgrounds")
public interface BackgroundRepository extends CrudRepository<Background, BigInteger> {

  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends Background> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Background entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Background> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Background', 'read')")
  Optional<Background> findById(@P("entityId") BigInteger entityId);

}