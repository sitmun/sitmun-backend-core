package org.sitmun.domain.cartography.permission;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography group")
@RepositoryRestResource(collectionResourceRel = "cartography-groups", path = "cartography-groups")
public interface CartographyPermissionRepository extends
  PagingAndSortingRepository<CartographyPermission, Integer>,
  QuerydslPredicateExecutor<CartographyPermission> {
}