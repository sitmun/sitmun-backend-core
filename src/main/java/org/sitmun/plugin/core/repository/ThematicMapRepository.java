package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.sitmun.plugin.core.domain.ThematicMap;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "thematic map")
@RepositoryRestResource(collectionResourceRel = "thematic-maps", path = "thematic-maps")
public interface ThematicMapRepository extends CrudRepository<ThematicMap, Integer> {

  @Override
    //@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends ThematicMap> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ThematicMap entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ThematicMap', 'delete')")
  void deleteById(@P("entityId") Integer entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ThematicMap> findAll();

  @Override
  @PostAuthorize("hasPermission(#entity, 'administration') or hasPermission(returnObject, 'read')")
  Optional<ThematicMap> findById(Integer id);

}