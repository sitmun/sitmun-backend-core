package org.sitmun.domain.cartography;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.role.Role;
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
  @Query(
      """
      SELECT DISTINCT car
      FROM CartographyPermission cp, Cartography car, CartographyAvailability cav, Role rol
      WHERE rol member of cp.roles AND rol in ?1
      AND car member of cp.members AND cav member of car.availabilities AND cav.territory.id = ?2
      """)
  List<Cartography> findByRolesAndTerritory(List<Role> roles, Integer territoryId);
}
