package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Date;
import java.util.Optional;

@Tag(name = "user")
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends PagingAndSortingRepository<User, Integer> {

  @RestResource(exported = false)
  Optional<User> findByUsername(String username);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends User> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull User entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.User', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(#filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<User> findAll();

  @Override
  @PostAuthorize("hasPermission(#returnObject, 'administration') or hasPermission(returnObject, 'read')")
  @NonNull
  Optional<User> findById(@NonNull Integer id);

  @RestResource(exported = false)
  @EntityGraph(attributePaths = "permissions")
  Optional<User> findOneWithPermissionsByUsername(String name);

  @Query(name = "dashboard.usersPerApplicationSinceDate")
  Iterable<Object[]> usersPerApplicationSinceDate(@Param("sinceDate") Date sinceDate);

  @Query(name = "dashboard.usersByCreatedDate")
  Iterable<Object[]> usersByCreatedDateSinceDate(@Param("sinceDate") Date sinceDate);
}