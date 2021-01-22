package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.Service;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

@Tag(name = "service")
@RepositoryRestResource(collectionResourceRel = "services", path = "services")
public interface ServiceRepository extends PagingAndSortingRepository<Service, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Service> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  @NonNull
  void delete(@P("entity") @NonNull Service entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service', 'delete')")
  @NonNull
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Service> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service', 'read')")
  @NonNull
  Optional<Service> findById(@P("entityId") @NonNull Integer entityId);

  @RestResource(exported = false)
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select service.layers from Service service where service.id =:id")
  List<Cartography> findLayers(@Param("id") Integer id);


}