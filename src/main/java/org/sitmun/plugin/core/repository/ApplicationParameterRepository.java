package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.ApplicationParameter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "application-parameters", path = "application-parameters")
public interface ApplicationParameterRepository extends CrudRepository<ApplicationParameter, BigInteger> {
  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  ApplicationParameter save(@P("entity") ApplicationParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ApplicationParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ApplicationParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter', 'read')")
  ApplicationParameter findOne(@P("entityId") BigInteger entityId);


}