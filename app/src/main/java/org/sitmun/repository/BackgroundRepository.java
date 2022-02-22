package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Background;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "background")
@RepositoryRestResource(collectionResourceRel = "backgrounds", path = "backgrounds")
public interface BackgroundRepository extends PagingAndSortingRepository<Background, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Background> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Background entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Background','administration') or hasPermission(#entityId, 'org.sitmun.domain.Background', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Background> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Background','administration') or hasPermission(#entityId, 'org.sitmun.domain.Background', 'read')")
  @NonNull
  Optional<Background> findById(@P("entityId") @NonNull Integer entityId);

}