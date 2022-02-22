package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.TaskAvailability;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "task availability")
@RepositoryRestResource(collectionResourceRel = "task-availabilities", path = "task-availabilities")
public interface TaskAvailabilityRepository extends
  PagingAndSortingRepository<TaskAvailability, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends TaskAvailability> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull TaskAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.TaskAvailability','administration') or hasPermission(#entityId, 'org.sitmun.domain.TaskAvailability', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<TaskAvailability> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.TaskAvailability','administration') or hasPermission(#entityId, 'org.sitmun.domain.TaskAvailability', 'read')")
  @NonNull
  Optional<TaskAvailability> findById(@P("entityId") @NonNull Integer entityId);


}