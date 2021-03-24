package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.DownloadTask;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "download task")
public interface DownloadTaskRepository extends
  PagingAndSortingRepository<DownloadTask, Integer>, QuerydslPredicateExecutor<DownloadTask> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends DownloadTask> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull DownloadTask entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.DownloadTask','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.DownloadTask', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<DownloadTask> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.DownloadTask','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.DownloadTask', 'read')")
  @NonNull
  Optional<DownloadTask> findById(@P("entityId") @NonNull Integer entityId);

}