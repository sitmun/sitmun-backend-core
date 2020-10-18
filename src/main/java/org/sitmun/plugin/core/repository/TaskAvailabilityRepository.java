package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.TaskAvailability;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "task availability")
@RepositoryRestResource(collectionResourceRel = "task-availabilities", path = "task-availabilities")
public interface TaskAvailabilityRepository extends CrudRepository<TaskAvailability, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends TaskAvailability> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") TaskAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<TaskAvailability> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TaskAvailability', 'read')")
  Optional<TaskAvailability> findById(@P("entityId") BigInteger entityId);


}