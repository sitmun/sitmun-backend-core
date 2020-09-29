package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@RepositoryRestResource(collectionResourceRel = "application-backgrounds", path = "application-backgrounds")
public interface ApplicationBackgroundRepository
    extends PagingAndSortingRepository<ApplicationBackground, BigInteger> {

  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends ApplicationBackground> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ApplicationBackground entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ApplicationBackground> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground', 'read')")
  Optional<ApplicationBackground> findById(@P("entityId") BigInteger entityId);

}