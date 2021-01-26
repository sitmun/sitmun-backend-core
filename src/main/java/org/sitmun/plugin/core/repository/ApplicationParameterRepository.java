package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.ApplicationParameter;
import org.sitmun.plugin.core.domain.QApplicationParameter;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "application parameter")
@RepositoryRestResource(collectionResourceRel = "application-parameters", path = "application-parameters")
public interface ApplicationParameterRepository
  extends PagingAndSortingRepository<ApplicationParameter, Integer>,
  QuerydslPredicateExecutor<ApplicationParameter>,
  QuerydslBinderCustomizer<QApplicationParameter> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends ApplicationParameter> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull ApplicationParameter entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<ApplicationParameter> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationParameter', 'read')")
  @NonNull
  Optional<ApplicationParameter> findById(@P("entityId") @NonNull Integer entityId);

  default void customize(@NonNull QuerydslBindings bindings, @NonNull QApplicationParameter root) {
  }
}