package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.TaskGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@RepositoryRestResource(collectionResourceRel = "task-groups", path = "task-groups")
public interface TaskGroupRepository extends CrudRepository<TaskGroup, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends TaskGroup> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") TaskGroup entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskGroup','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskGroup', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<TaskGroup> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskGroup','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskGroup', 'read')")
  Optional<TaskGroup> findById(@P("entityId") BigInteger entityId);
}