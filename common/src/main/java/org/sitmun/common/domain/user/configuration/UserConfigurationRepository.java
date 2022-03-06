package org.sitmun.common.domain.user.configuration;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.common.domain.user.User;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@Tag(name = "user configuration")
@RepositoryRestResource(collectionResourceRel = "user-configurations", path = "user-configurations")
public interface UserConfigurationRepository extends
  PagingAndSortingRepository<UserConfiguration, Integer>,
  QuerydslPredicateExecutor<UserConfiguration>,
  QuerydslBinderCustomizer<QUserConfiguration> {

  List<UserConfiguration> findByUser(User currentUser);

  @Override
  default void customize(QuerydslBindings bindings, QUserConfiguration root) {
    //noinspection NullableProblems
    bindings.bind(root.role.id).first(SimpleExpression::eq);
  }
}