package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.TerritoryGroupType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "territory group type")
@RepositoryRestResource(collectionResourceRel = "territory-group-types", path = "territory-group-types")
public interface TerritoryGroupTypeRepository
    extends CrudRepository<TerritoryGroupType, BigInteger> {
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends TerritoryGroupType> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") TerritoryGroupType entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TerritoryGroupType', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<TerritoryGroupType> findAll();

  @Override
  @PostAuthorize("hasPermission(#entity, 'administration') or hasPermission(returnObject, 'read')")
  Optional<TerritoryGroupType> findById(BigInteger id);

  @RestResource(exported = false)
  Optional<TerritoryGroupType> findOneByName(String name);

}