package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "task-parameters", path = "task-parameters")
public interface TaskParameterRepository extends CrudRepository<TaskParameter, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  TaskParameter save(@P("entity") TaskParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") TaskParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<TaskParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskParameter', 'read')")
  TaskParameter findOne(@P("entityId") BigInteger entityId);
}