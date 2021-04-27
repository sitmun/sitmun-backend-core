package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.ConfigurationParameter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

@Tag(name = "configuration parameter")
@RepositoryRestResource(collectionResourceRel = "configuration-parameters", path = "configuration-parameters")
public interface ConfigurationParameterRepository
  extends PagingAndSortingRepository<ConfigurationParameter, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends ConfigurationParameter> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull ConfigurationParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ConfigurationParameter', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  List<ConfigurationParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ConfigurationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ConfigurationParameter', 'read')")
  @NonNull
  Optional<ConfigurationParameter> findById(@P("entityId") @NonNull Integer entityId);
}