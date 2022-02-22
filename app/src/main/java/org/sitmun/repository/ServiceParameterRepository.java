package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.ServiceParameter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "service parameter")
@RepositoryRestResource(collectionResourceRel = "service-parameters", path = "service-parameters")
public interface ServiceParameterRepository extends
  PagingAndSortingRepository<ServiceParameter, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends ServiceParameter> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull ServiceParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.ServiceParameter','administration') or hasPermission(#entityId, 'org.sitmun.domain.ServiceParameter', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<ServiceParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.ServiceParameter','administration') or hasPermission(#entityId, 'org.sitmun.domain.ServiceParameter', 'read')")
  @NonNull
  Optional<ServiceParameter> findById(@P("entityId") @NonNull Integer entityId);
}