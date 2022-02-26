package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Cartography;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;

import java.util.Date;

@Tag(name = "cartography")
@RepositoryRestResource(
  collectionResourceRel = "cartographies",
  itemResourceRel = "cartography",
  path = "cartographies")
public interface CartographyRepository extends PagingAndSortingRepository<Cartography, Integer> {

  @Query("select cartography from Cartography cartography left join fetch cartography.service where cartography.id =:id")
  Cartography findOneWithEagerRelationships(@Param("id") Integer id);

  @Query(name = "dashboard.cartographiesByCreatedDate")
  Iterable<Object[]> cartographiesByCreatedDateSinceDate(@Param("sinceDate") Date sinceDate);

  @Query("select distinct cartography from Cartography cartography, Application app, Role role, CartographyPermission permission " +
    "where app.id = :applicationId and role member of app.availableRoles and role member of permission.roles and permission member of cartography.permissions")
  Iterable<Cartography> available(@Param("applicationId") @NonNull Integer applicationId);
}