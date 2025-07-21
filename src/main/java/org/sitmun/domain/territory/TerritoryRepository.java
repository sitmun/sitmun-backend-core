package org.sitmun.domain.territory;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "territory")
@RepositoryRestResource(collectionResourceRel = "territories", path = "territories")
public interface TerritoryRepository extends JpaRepository<Territory, Integer> {

  @RestResource(exported = false)
  @Query("SELECT DISTINCT t FROM Territory t WHERE " +
    "t.id IN (SELECT uc.territory.id FROM UserConfiguration uc, Application app WHERE app.id = ?2 AND uc.user.username = ?1 AND uc.role member app.availableRoles AND uc.appliesToChildrenTerritories = false) " +
    "OR t.id IN (SELECT uc.territory.id FROM UserConfiguration uc, Application app WHERE app.id = ?2 AND uc.user.username = ?1 AND uc.role member app.availableRoles AND uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true) " +
    "OR t.id IN (SELECT childTerritory.id FROM Territory childTerritory, UserConfiguration uc, Application app WHERE childTerritory member uc.territory.members AND  app.id = ?2 AND uc.user.username = ?1 AND uc.role member app.availableRoles AND uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)")
  Page<Territory> findByUserAndApplication(String username, Integer appId, Pageable pageable);

  @RestResource(exported = false)
  @Query("SELECT DISTINCT t FROM Territory t WHERE " +
    "t.id IN (SELECT uc.territory.id FROM UserConfiguration uc, Application app WHERE uc.user.username = ?1 AND uc.role member app.availableRoles AND uc.appliesToChildrenTerritories = false) " +
    "OR t.id IN (SELECT uc.territory.id FROM UserConfiguration uc, Application app WHERE uc.user.username = ?1 AND uc.role member app.availableRoles AND uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true) " +
    "OR t.id IN (SELECT childTerritory.id FROM Territory childTerritory, UserConfiguration uc, Application app WHERE childTerritory member uc.territory.members AND uc.user.username = ?1 AND uc.role member app.availableRoles AND uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)")
  Page<Territory> findByUser(String username, Pageable pageable);
}