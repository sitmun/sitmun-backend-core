package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "application-backgrounds", path = "application-backgrounds")
public interface ApplicationBackgroundRepository extends PagingAndSortingRepository<ApplicationBackground, BigInteger> {
  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  ApplicationBackground save(@P("entity") ApplicationBackground entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ApplicationBackground entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ApplicationBackground> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground', 'read')")
  ApplicationBackground findOne(@P("entityId") BigInteger entityId);

}