package org.sitmun.plugin.core.repository;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.QUserConfiguration;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

@Tag(name = "user configuration")
@RepositoryRestResource(collectionResourceRel = "user-configurations", path = "user-configurations")
public interface UserConfigurationRepository extends
  PagingAndSortingRepository<UserConfiguration, Integer>,
  QuerydslPredicateExecutor<UserConfiguration>,
  QuerydslBinderCustomizer<QUserConfiguration> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends UserConfiguration> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull UserConfiguration entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.UserConfiguration', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<UserConfiguration> findAll();

  @Override
  @PostAuthorize("hasPermission(returnObject, 'read')")
  @NonNull
  Optional<UserConfiguration> findById(@NonNull Integer id);

  @Override
  default void customize(QuerydslBindings bindings, QUserConfiguration root) {
    //noinspection NullableProblems
    bindings.bind(root.role.id).first(SimpleExpression::eq);
  }

  List<UserConfiguration> findByUser(User currentUser);
}