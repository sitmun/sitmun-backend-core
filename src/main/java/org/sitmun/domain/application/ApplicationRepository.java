package org.sitmun.domain.application;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;


@Tag(name = "application")
@RepositoryRestResource(collectionResourceRel = "applications", path = "applications"/*, excerptProjection = ApplicationProjection.class*/)
public interface ApplicationRepository extends PagingAndSortingRepository<Application, Integer> {

  @RestResource(exported = false)
  @Query("select distinct app.id, uc.territory.id from Application app, UserConfiguration uc where uc.role member app.availableRoles")
  List<Object[]> listIdApplicationsPerTerritories();

  @RestResource(exported = false)
  @Query("select app from Application app where app.id in (select distinct app.id from Application app, UserConfiguration uc where uc.role member app.availableRoles and uc.user.username = ?1)")
  Page<Application> findByUser(String username, Pageable pageable);

  @RestResource(exported = false)
  @Query("select distinct app from Application app where app.id in (select distinct app.id from Application app, UserConfiguration uc where uc.role member app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?2 and uc.appliesToChildrenTerritories = false) " +
    "or app.id in (select distinct app.id from Application app, UserConfiguration uc where uc.role member app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?2 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true) " +
    "or app.id in (select distinct app.id from Application app, UserConfiguration uc where uc.role member app.availableRoles and uc.user.username = ?1 and ?2 in (select childTerritory.id from Territory childTerritory where childTerritory member uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)")
  Page<Application> findByUserAndTerritory(String username, Integer territoryId, Pageable pageable);

  @RestResource(exported = false)
  @Query("select distinct app from Application app where app.id in (select distinct app.id from Application app, UserConfiguration uc where app.id = ?2 and uc.role member app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = false) " +
    "or app.id in (select distinct app.id from Application app, UserConfiguration uc where app.id = ?2 and uc.role member app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true) " +
    "or app.id in (select distinct app.id from Application app, UserConfiguration uc where app.id = ?2 and uc.role member app.availableRoles and uc.user.username = ?1 and ?3 in (select childTerritory.id from Territory childTerritory where childTerritory member uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)")
  Optional<Application> findByIdAndUserAndTerritory(String username, Integer appId, Integer territoryId);
}