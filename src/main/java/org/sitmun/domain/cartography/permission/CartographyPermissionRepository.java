package org.sitmun.domain.cartography.permission;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "cartography group")
@RepositoryRestResource(collectionResourceRel = "cartography-groups", path = "cartography-groups")
public interface CartographyPermissionRepository
    extends JpaRepository<CartographyPermission, Integer>,
        QuerydslPredicateExecutor<CartographyPermission> {

  @RestResource(exported = false)
  @Query(
      """
      SELECT DISTINCT cp
      FROM CartographyPermission cp, Cartography car, CartographyAvailability cav, Role rol
      WHERE rol member of cp.roles AND rol in ?1
      AND car member of cp.members AND cav member of car.availabilities AND cav.territory.id = ?2
      """)
  List<CartographyPermission> findByRolesAndTerritory(List<Role> roles, Integer territoryId);
}
