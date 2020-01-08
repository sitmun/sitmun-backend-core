package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.ServiceParameter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "service-parameters", path = "service-parameters")
public interface ServiceParameterRepository extends CrudRepository<ServiceParameter, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
    //@PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  ServiceParameter save(@P("entity") ServiceParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ServiceParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ServiceParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ServiceParameter', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ServiceParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ServiceParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ServiceParameter', 'read')")
  ServiceParameter findOne(@P("entityId") BigInteger entityId);
}