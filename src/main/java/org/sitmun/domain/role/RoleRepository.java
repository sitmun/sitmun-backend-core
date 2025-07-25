package org.sitmun.domain.role;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "role")
@RepositoryRestResource(collectionResourceRel = "roles", path = "roles")
public interface RoleRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Role, Integer> {

  @RestResource(exported = false)
  @Query(
      """
      select distinct role from Role role
      where role.id in (select distinct uc.role from Application app, UserConfiguration uc
        where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = false)
      or role.id in (select distinct uc.role from Application app, UserConfiguration uc
        where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and uc.territory.id = ?3 and uc.appliesToChildrenTerritories = true and app.accessParentTerritory = true)
      or role.id in (select distinct uc.role from Application app, UserConfiguration uc
        where app.id = ?2 and uc.role member of app.availableRoles and uc.user.username = ?1 and ?3 in (select childTerritory.id from Territory childTerritory where childTerritory member of uc.territory.members) and uc.appliesToChildrenTerritories = true and app.accessChildrenTerritory = true)
      """)
  List<Role> findRolesByApplicationAndUserAndTerritory(
      String username, Integer appId, Integer territoryId);
}
