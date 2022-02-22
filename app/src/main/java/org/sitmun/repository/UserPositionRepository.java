package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.UserPosition;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "user position")
@RepositoryRestResource(collectionResourceRel = "user-positions", path = "user-positions")
public interface UserPositionRepository extends
  PagingAndSortingRepository<UserPosition, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends UserPosition> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull UserPosition entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.UserPosition', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(#filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<UserPosition> findAll();

  @Override
  @PostAuthorize("hasPermission(#returnObject, 'administration') or hasPermission(returnObject, 'read')")
  @NonNull
  Optional<UserPosition> findById(@NonNull Integer id);
}