package org.sitmun.plugin.core.repository;

import java.util.Optional;
import org.sitmun.plugin.core.domain.ThematicMapRange;
import org.sitmun.plugin.core.domain.ThematicMapRangeId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@RepositoryRestResource(collectionResourceRel = "thematic-map-ranges", path = "thematic-map-ranges")
public interface ThematicMapRangeRepository
    extends CrudRepository<ThematicMapRange, ThematicMapRangeId> {

  @Override
    //@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends ThematicMapRange> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") ThematicMapRange entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ThematicMapRange', 'delete')")
  void deleteById(@P("entityId") ThematicMapRangeId entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<ThematicMapRange> findAll();

  @Override
  @PostAuthorize("hasPermission(#entity, 'administration') or hasPermission(returnObject, 'read')")
  Optional<ThematicMapRange> findById(ThematicMapRangeId id);

}