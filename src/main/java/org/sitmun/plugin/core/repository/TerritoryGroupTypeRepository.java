package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.TerritoryGroupType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "territory group type")
@RepositoryRestResource(collectionResourceRel = "territory-group-types", path = "territory-group-types")
@Deprecated
public interface TerritoryGroupTypeRepository
  extends PagingAndSortingRepository<TerritoryGroupType, Integer> {
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends TerritoryGroupType> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull TerritoryGroupType entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TerritoryGroupType', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<TerritoryGroupType> findAll();

  @Override
  @PostAuthorize("hasPermission(returnObject, 'administration') or hasPermission(returnObject, 'read')")
  @NonNull
  Optional<TerritoryGroupType> findById(@NonNull Integer id);

  @RestResource(exported = false)
  Optional<TerritoryGroupType> findOneByName(String name);

}