package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.Territory;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "territory")
@RepositoryRestResource(collectionResourceRel = "territories", path = "territories")
public interface TerritoryRepository extends PagingAndSortingRepository<Territory, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Territory> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Territory entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Territory> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Territory', 'read')")
  @NonNull
  Optional<Territory> findById(@P("entityId") @NonNull Integer entityId);

  @RestResource(exported = false)
  Optional<Territory> findOneByName(String name);


}