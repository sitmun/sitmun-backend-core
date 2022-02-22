package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.ThematicMap;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "thematic map")
public interface ThematicMapRepository extends PagingAndSortingRepository<ThematicMap, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends ThematicMap> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull ThematicMap entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.ThematicMap', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<ThematicMap> findAll();

  @Override
  @PostAuthorize("hasPermission(returnObject, 'administration') or hasPermission(returnObject, 'read')")
  @NonNull
  Optional<ThematicMap> findById(@NonNull Integer id);

}