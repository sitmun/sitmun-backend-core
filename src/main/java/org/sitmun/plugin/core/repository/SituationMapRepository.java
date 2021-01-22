package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.SituationMap;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "situation map")
@RepositoryRestResource(collectionResourceRel = "situation-maps", path = "situation-maps")
public interface SituationMapRepository extends
  PagingAndSortingRepository<SituationMap, Integer>,
  QuerydslPredicateExecutor<SituationMap> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends SituationMap> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull SituationMap entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.SituationMap','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.SituationMap', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostAuthorize("hasPermission(returnObject, 'administration')")
  @PostFilter("hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<SituationMap> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.SituationMap','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.SituationMap', 'read')")
  @NonNull
  Optional<SituationMap> findById(@P("entityId") @NonNull Integer entityId);
}