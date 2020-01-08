package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import org.sitmun.plugin.core.domain.TaskAvailability;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "task-availabilities", path = "task-availabilities")
public interface TaskAvailabilityRepository extends CrudRepository<TaskAvailability, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  TaskAvailability save(@P("entity") TaskAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") TaskAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<TaskAvailability> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability', 'read')")
  TaskAvailability findOne(@P("entityId") BigInteger entityId);


}