package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.TaskGroup;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "task group")
@RepositoryRestResource(collectionResourceRel = "task-groups", path = "task-groups")
public interface TaskGroupRepository extends PagingAndSortingRepository<TaskGroup, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends TaskGroup> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull TaskGroup entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.TaskGroup','administration') or hasPermission(#entityId, 'org.sitmun.domain.TaskGroup', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<TaskGroup> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.TaskGroup','administration') or hasPermission(#entityId, 'org.sitmun.domain.TaskGroup', 'read')")
  @NonNull
  Optional<TaskGroup> findById(@P("entityId") @NonNull Integer entityId);
}