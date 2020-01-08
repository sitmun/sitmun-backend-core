package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "user-configurations", path = "user-configurations")
public interface UserConfigurationRepository extends CrudRepository<UserConfiguration, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
    //@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  UserConfiguration save(@P("entity") UserConfiguration entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") UserConfiguration entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.UserConfiguration', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<UserConfiguration> findAll();

  @Override
  @PostAuthorize("hasPermission(#entity, 'administration') or hasPermission(returnObject, 'read')")
  UserConfiguration findOne(BigInteger id);


}