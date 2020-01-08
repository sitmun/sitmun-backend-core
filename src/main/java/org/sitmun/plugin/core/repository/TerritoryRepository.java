package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Territory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "territories", path = "territories")
public interface TerritoryRepository extends CrudRepository<Territory, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  Territory save(@P("entity") Territory entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Territory entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Territory> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory', 'read')")
  Territory findOne(@P("entityId") BigInteger entityId);

  @RestResource(exported = false)
  Optional<Territory> findOneByName(String name);


}