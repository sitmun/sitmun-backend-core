package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "task parameter")
public interface TaskParameterRepository extends
  PagingAndSortingRepository<TaskParameter, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends TaskParameter> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull TaskParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<TaskParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter', 'read')")
  @NonNull
  Optional<TaskParameter> findById(@P("entityId") @NonNull Integer entityId);

  void deleteAllByTask(@NonNull Task task);

  Iterable<TaskParameter> findAllByTask(@NonNull Task task);
}