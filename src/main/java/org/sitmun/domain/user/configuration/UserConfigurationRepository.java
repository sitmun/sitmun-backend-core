package org.sitmun.domain.user.configuration;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "user configuration")
@RepositoryRestResource(collectionResourceRel = "user-configurations", path = "user-configurations")
public interface UserConfigurationRepository
    extends JpaRepository<UserConfiguration, Integer>,
        QuerydslPredicateExecutor<UserConfiguration>,
        QuerydslBinderCustomizer<QUserConfiguration> {

  List<UserConfiguration> findByUser(User currentUser);

  @Override
  default void customize(QuerydslBindings querydslBindings, QUserConfiguration root) {
    querydslBindings.bind(root.role.id).first(SimpleExpression::eq);
  }
}
