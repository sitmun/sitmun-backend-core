package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.DatabaseConnection;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "connection")
@RepositoryRestResource(collectionResourceRel = "connections", path = "connections")
public interface DatabaseConnectionRepository extends PagingAndSortingRepository<DatabaseConnection, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends DatabaseConnection> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull DatabaseConnection entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.DatabaseConnection','administration') or hasPermission(#entityId, 'org.sitmun.domain.DatabaseConnection', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<DatabaseConnection> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.DatabaseConnection','administration') or hasPermission(#entityId, 'org.sitmun.domain.DatabaseConnection', 'read')")
  @NonNull
  Optional<DatabaseConnection> findById(@P("entityId") @NonNull Integer entityId);

}