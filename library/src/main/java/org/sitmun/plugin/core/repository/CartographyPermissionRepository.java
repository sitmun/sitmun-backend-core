package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyPermission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

@Tag(name = "cartography group")
@RepositoryRestResource(collectionResourceRel = "cartography-groups", path = "cartography-groups")
public interface CartographyPermissionRepository extends
  PagingAndSortingRepository<CartographyPermission, Integer>,
  QuerydslPredicateExecutor<CartographyPermission> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends CartographyPermission> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull CartographyPermission entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyPermission','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyPermission', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostAuthorize("hasPermission(returnObject, 'administration')")
  @PostFilter("hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<CartographyPermission> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyPermission','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyPermission', 'read')")
  @NonNull
  Optional<CartographyPermission> findById(@P("entityId") @NonNull Integer entityId);

  @RestResource(exported = false)
  @PostAuthorize("hasPermission(returnObject, 'administration')")
  @PostFilter("hasPermission(filterObject, 'read')")
  @Query("select CartographyPermission.members from CartographyPermission CartographyPermission where CartographyPermission.id =:id")
  List<Cartography> findCartographyMembers(@Param("id") Integer id);


}