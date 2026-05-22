package org.sitmun.domain.cartography.permission;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
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
      FROM CartographyPermission cp
      JOIN cp.roles rol
      JOIN cp.members car
      JOIN car.availabilities cav
      WHERE rol in ?1 AND cav.territory.id = ?2
      """)
  List<CartographyPermission> findByRolesAndTerritory(List<Role> roles, Integer territoryId);

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select cartographyPermission
      from CartographyPermission cartographyPermission
      where (lower(cartographyPermission.name) like lower(concat('%', :q, '%')))
      and (:excludedType is null or cartographyPermission.type <> :excludedType)
      """)
  Page<CartographyPermission> findByContent(@Param("q") String q, @Param("excludedType") String excludedType, Pageable pageable);
}
