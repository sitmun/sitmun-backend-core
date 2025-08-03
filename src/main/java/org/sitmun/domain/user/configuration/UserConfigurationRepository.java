package org.sitmun.domain.user.configuration;

import com.querydsl.core.types.dsl.SimpleExpression;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "user configuration")
@RepositoryRestResource(collectionResourceRel = "user-configurations", path = "user-configurations")
public interface UserConfigurationRepository
    extends JpaRepository<UserConfiguration, Integer>,
        QuerydslPredicateExecutor<UserConfiguration>,
        QuerydslBinderCustomizer<QUserConfiguration> {

  List<UserConfiguration> findByUser(User currentUser);

  @RestResource(exported = false)
  @Query(
      "SELECT COUNT(uc) > 0 FROM UserConfiguration uc WHERE uc.user.username = ?1 AND uc.role IN ?2")
  boolean existsByUserUsernameAndRoleIn(String username, Set<Role> roles);

  @Override
  default void customize(QuerydslBindings querydslBindings, QUserConfiguration root) {
    querydslBindings.bind(root.role.id).first(SimpleExpression::eq);
  }
}
