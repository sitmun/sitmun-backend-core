package org.sitmun.domain.cartography.permission;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.role.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@Tag(name = "cartography group")
@RepositoryRestResource(collectionResourceRel = "cartography-groups", path = "cartography-groups")
public interface CartographyPermissionRepository extends
  PagingAndSortingRepository<CartographyPermission, Integer>,
  QuerydslPredicateExecutor<CartographyPermission> {

  @RestResource(exported = false)
  @Query("SELECT DISTINCT cp FROM CartographyPermission cp, Cartography car, CartographyAvailability cav, Role rol WHERE " +
    "rol member cp.roles AND rol in ?1 " +                                                    // CartographyPermission is linked to any of the roles
    "AND car member cp.members AND cav member car.availabilities AND cav.territory.id = ?2")  // AND the cartography is available to the territory
  List<CartographyPermission> findByRolesAndTerritory(List<Role> roles, Integer territoryId);
}