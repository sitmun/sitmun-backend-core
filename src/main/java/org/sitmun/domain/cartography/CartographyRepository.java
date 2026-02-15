package org.sitmun.domain.cartography;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.sitmun.domain.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;

@Tag(name = "cartography")
@RepositoryRestResource(
    collectionResourceRel = "cartographies",
    itemResourceRel = "cartography",
    path = "cartographies")
public interface CartographyRepository extends JpaRepository<Cartography, Integer> {

  @Override
  @EntityGraph(attributePaths = {"service", "spatialSelectionService", "spatialSelectionConnection", "styles"})
  Page<Cartography> findAll(Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {
        "service",
        "permissions",
        "availabilities",
        "styles",
        "filters",
        "parameters",
        "spatialSelectionParameters",
        "treeNodes"
      })
  Optional<Cartography> findById(Integer id);

  @Query(
      "select cartography from Cartography cartography left join fetch cartography.service where cartography.id =:id")
  Cartography findOneWithEagerRelationships(@Param("id") Integer id);

  @Query(
      """
      select distinct cartography
      from Cartography cartography, Application app, Role role, CartographyPermission permission
      where app.id = :applicationId
      and role member of app.availableRoles
      and role member of permission.roles
      and permission member of cartography.permissions
      """)
  Iterable<Cartography> available(@Param("applicationId") @NonNull Integer applicationId);

  @RestResource(exported = false)
  @EntityGraph(attributePaths = {"service"})
  @Query(
      """
      SELECT DISTINCT car
      FROM CartographyPermission cp
      JOIN cp.members car
      JOIN cp.roles rol
      JOIN car.availabilities cav
      WHERE rol in ?1 AND cav.territory.id = ?2
      """)
  List<Cartography> findByRolesAndTerritory(List<Role> roles, Integer territoryId);
}
