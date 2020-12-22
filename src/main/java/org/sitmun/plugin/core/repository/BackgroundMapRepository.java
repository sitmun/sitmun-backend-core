package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.sitmun.plugin.core.domain.BackgroundMap;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "background map")
@RepositoryRestResource(collectionResourceRel = "background-maps", path = "background-maps")
public interface BackgroundMapRepository extends
    PagingAndSortingRepository<BackgroundMap, Integer>,
    QuerydslPredicateExecutor<BackgroundMap> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends BackgroundMap> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull BackgroundMap entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.BackgroundMap','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.BackgroundMap', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostAuthorize("hasPermission(returnObject, 'administration')")
  @PostFilter("hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<BackgroundMap> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.BackgroundMap','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.BackgroundMap', 'read')")
  @NonNull
  Optional<BackgroundMap> findById(@P("entityId") @NonNull Integer entityId);
}