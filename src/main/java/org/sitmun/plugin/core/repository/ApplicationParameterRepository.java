package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.ApplicationParameter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "application parameter")
@RepositoryRestResource(collectionResourceRel = "application-parameters", path = "application-parameters")
public interface ApplicationParameterRepository
    extends CrudRepository<ApplicationParameter, BigInteger> {

  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends ApplicationParameter> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ApplicationParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ApplicationParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter', 'read')")
  Optional<ApplicationParameter> findById(@P("entityId") BigInteger entityId);


}